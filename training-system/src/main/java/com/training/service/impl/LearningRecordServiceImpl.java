package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.ChapterProgressDTO;
import com.training.entity.dto.CourseProgressDTO;
import com.training.entity.dto.HeartbeatRequest;
import com.training.mapper.*;
import com.training.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private final LearningRecordMapper learningRecordMapper;
    private final CoursewareMapper coursewareMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LEARNING_CACHE_PREFIX = "learning:";
    private static final long CACHE_TTL_HOURS = 1;

    @Override
    @Transactional
    public void heartbeat(Long studentId, HeartbeatRequest req) {
        Long coursewareId = req.getCoursewareId();

        // 1. Find or create LearningRecord by studentId + coursewareId (UNIQUE constraint)
        LearningRecord record = learningRecordMapper.selectOne(
                new LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getStudentId, studentId)
                        .eq(LearningRecord::getCoursewareId, coursewareId)
        );

        if (record == null) {
            record = new LearningRecord();
            record.setStudentId(studentId);
            record.setCourseId(req.getCourseId());
            record.setChapterId(req.getChapterId());
            record.setCoursewareId(coursewareId);
            record.setStudyDurationSeconds(0);
            record.setLastPositionSeconds(0);
            record.setProgressPercent(0.0);
            record.setStatus("NOT_STARTED");
            learningRecordMapper.insert(record);
        }

        // 2. Get Courseware entity for duration info
        Courseware courseware = coursewareMapper.selectById(coursewareId);
        if (courseware == null) {
            throw new BusinessException("课件不存在: " + coursewareId);
        }

        // 3. Update study_duration_seconds += req.studyDurationSeconds (default 30 if null)
        int durationIncrement = req.getStudyDurationSeconds() != null ? req.getStudyDurationSeconds() : 30;
        record.setStudyDurationSeconds(record.getStudyDurationSeconds() + durationIncrement);

        // 4. Update last_position_seconds = req.currentPositionSeconds
        if (req.getCurrentPositionSeconds() != null) {
            record.setLastPositionSeconds(req.getCurrentPositionSeconds());
        }

        // 5. Calculate progress_percent
        double progressPercent;
        int durationSeconds = courseware.getDurationSeconds();
        if (durationSeconds == 0) {
            // If durationSeconds=0 (like PDF), set 100% when heartbeat received
            progressPercent = 100.0;
        } else {
            progressPercent = Math.min(100.0,
                    (double) record.getStudyDurationSeconds() * 100 / durationSeconds);
        }
        record.setProgressPercent(progressPercent);

        // 6. Update status transitions
        LocalDateTime now = LocalDateTime.now();
        if ("NOT_STARTED".equals(record.getStatus())) {
            // NOT_STARTED → IN_PROGRESS on first heartbeat
            record.setStatus("IN_PROGRESS");
            record.setStartedAt(now);
        }
        if (progressPercent >= 100.0 && !"COMPLETED".equals(record.getStatus())) {
            // IN_PROGRESS → COMPLETED when progress >= 100
            record.setStatus("COMPLETED");
            record.setCompletedAt(now);
        }

        // 7. Set startedAt if first heartbeat (already handled above for NOT_STARTED→IN_PROGRESS)
        // completedAt when progress reaches 100% (already handled above)

        // 8. Cache in Redis: key="learning:{studentId}:{coursewareId}" value=progressPercent, TTL 1 hour
        String cacheKey = LEARNING_CACHE_PREFIX + studentId + ":" + coursewareId;
        redisTemplate.opsForValue().set(cacheKey, progressPercent, CACHE_TTL_HOURS, TimeUnit.HOURS);

        // 9. Save to DB
        learningRecordMapper.updateById(record);
    }

    @Override
    public CourseProgressDTO getCourseProgress(Long studentId, Long courseId) {
        // 1. Get course info
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在: " + courseId);
        }

        // 2. Get all chapters for the course
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getCourseId, courseId)
                        .orderByAsc(Chapter::getSortOrder)
        );
        if (chapters.isEmpty()) {
            return CourseProgressDTO.builder()
                    .courseId(courseId)
                    .courseTitle(course.getTitle())
                    .overallProgress(0.0)
                    .totalChapters(0)
                    .completedChapters(0)
                    .totalCoursewares(0)
                    .completedCoursewares(0)
                    .totalStudyDurationSeconds(0)
                    .build();
        }

        List<Long> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());

        // 3. Get all coursewares for those chapters
        List<Courseware> coursewares = coursewareMapper.selectList(
                new LambdaQueryWrapper<Courseware>()
                        .in(Courseware::getChapterId, chapterIds)
        );
        if (coursewares.isEmpty()) {
            return CourseProgressDTO.builder()
                    .courseId(courseId)
                    .courseTitle(course.getTitle())
                    .overallProgress(0.0)
                    .totalChapters(chapters.size())
                    .completedChapters(0)
                    .totalCoursewares(0)
                    .completedCoursewares(0)
                    .totalStudyDurationSeconds(0)
                    .build();
        }

        List<Long> coursewareIds = coursewares.stream().map(Courseware::getId).collect(Collectors.toList());

        // Build courseware→chapter mapping
        Map<Long, Long> coursewareChapterMap = coursewares.stream()
                .collect(Collectors.toMap(Courseware::getId, Courseware::getChapterId));

        // 4. Get all learning records for the student + those coursewares
        List<LearningRecord> records = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getStudentId, studentId)
                        .in(LearningRecord::getCoursewareId, coursewareIds)
        );
        Map<Long, LearningRecord> recordMap = records.stream()
                .collect(Collectors.toMap(LearningRecord::getCoursewareId, r -> r));

        // 5. Calculate completed chapters (all coursewares 100%) and overall progress
        int totalCoursewares = coursewares.size();
        int completedCoursewares = 0;
        int totalStudyDuration = 0;
        double totalProgressSum = 0.0;

        // Group coursewares by chapter
        Map<Long, List<Courseware>> chapterCoursewares = coursewares.stream()
                .collect(Collectors.groupingBy(Courseware::getChapterId));

        int completedChapters = 0;
        for (Chapter chapter : chapters) {
            List<Courseware> chapterCws = chapterCoursewares.getOrDefault(chapter.getId(), Collections.emptyList());
            boolean allCompleted = !chapterCws.isEmpty();
            for (Courseware cw : chapterCws) {
                LearningRecord lr = recordMap.get(cw.getId());
                if (lr != null) {
                    totalProgressSum += lr.getProgressPercent();
                    totalStudyDuration += lr.getStudyDurationSeconds();
                    if (lr.getProgressPercent() >= 100.0) {
                        completedCoursewares++;
                    } else {
                        allCompleted = false;
                    }
                } else {
                    allCompleted = false;
                }
            }
            if (allCompleted) {
                completedChapters++;
            }
        }

        double overallProgress = totalCoursewares > 0
                ? Math.min(100.0, totalProgressSum / totalCoursewares)
                : 0.0;

        return CourseProgressDTO.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .overallProgress(Math.round(overallProgress * 100.0) / 100.0)
                .totalChapters(chapters.size())
                .completedChapters(completedChapters)
                .totalCoursewares(totalCoursewares)
                .completedCoursewares(completedCoursewares)
                .totalStudyDurationSeconds(totalStudyDuration)
                .build();
    }

    @Override
    public List<ChapterProgressDTO> getChapterProgress(Long studentId, Long courseId) {
        // 1. Get chapters for course, ordered by sortOrder
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getCourseId, courseId)
                        .orderByAsc(Chapter::getSortOrder)
        );
        if (chapters.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());

        // 2. Get all coursewares for those chapters
        List<Courseware> coursewares = coursewareMapper.selectList(
                new LambdaQueryWrapper<Courseware>()
                        .in(Courseware::getChapterId, chapterIds)
        );

        if (coursewares.isEmpty()) {
            return chapters.stream().map(ch -> ChapterProgressDTO.builder()
                    .chapterId(ch.getId())
                    .chapterTitle(ch.getTitle())
                    .sortOrder(ch.getSortOrder())
                    .progress(0.0)
                    .totalCoursewares(0)
                    .completedCoursewares(0)
                    .studyDurationSeconds(0)
                    .build()
            ).collect(Collectors.toList());
        }

        List<Long> coursewareIds = coursewares.stream().map(Courseware::getId).collect(Collectors.toList());

        // 3. Get learning records for the student
        List<LearningRecord> records = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getStudentId, studentId)
                        .in(LearningRecord::getCoursewareId, coursewareIds)
        );
        Map<Long, LearningRecord> recordMap = records.stream()
                .collect(Collectors.toMap(LearningRecord::getCoursewareId, r -> r));

        // Group coursewares by chapter
        Map<Long, List<Courseware>> chapterCoursewares = coursewares.stream()
                .collect(Collectors.groupingBy(Courseware::getChapterId));

        // 4. Calculate per-chapter progress
        return chapters.stream().map(chapter -> {
            List<Courseware> chapterCws = chapterCoursewares.getOrDefault(chapter.getId(), Collections.emptyList());
            int totalCws = chapterCws.size();
            int completedCws = 0;
            int studyDuration = 0;
            double progressSum = 0.0;

            for (Courseware cw : chapterCws) {
                LearningRecord lr = recordMap.get(cw.getId());
                if (lr != null) {
                    progressSum += lr.getProgressPercent();
                    studyDuration += lr.getStudyDurationSeconds();
                    if (lr.getProgressPercent() >= 100.0) {
                        completedCws++;
                    }
                }
            }

            double progress = totalCws > 0
                    ? Math.min(100.0, progressSum / totalCws)
                    : 0.0;

            return ChapterProgressDTO.builder()
                    .chapterId(chapter.getId())
                    .chapterTitle(chapter.getTitle())
                    .sortOrder(chapter.getSortOrder())
                    .progress(Math.round(progress * 100.0) / 100.0)
                    .totalCoursewares(totalCws)
                    .completedCoursewares(completedCws)
                    .studyDurationSeconds(studyDuration)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public double getCompletionRate(Long studentId, Long courseId) {
        return getCourseProgress(studentId, courseId).getOverallProgress();
    }
}
