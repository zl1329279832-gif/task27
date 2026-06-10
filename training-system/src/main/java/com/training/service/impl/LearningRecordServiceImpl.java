package com.training.service.impl;

import com.training.dto.response.ChapterProgressDTO;
import com.training.dto.response.CourseProgressDTO;
import com.training.entity.Chapter;
import com.training.entity.Course;
import com.training.entity.Courseware;
import com.training.entity.LearningRecord;
import com.training.enums.LearningStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ChapterMapper;
import com.training.mapper.CourseMapper;
import com.training.mapper.CoursewareMapper;
import com.training.mapper.LearningRecordMapper;
import com.training.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private final LearningRecordMapper learningRecordMapper;
    private final CoursewareMapper coursewareMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    @Override
    public LearningRecord updateProgress(Long studentId, Long coursewareId, Integer progress,
                                          Integer lastPosition, Integer studyDuration) {
        log.info("更新学习进度, studentId: {}, coursewareId: {}, progress: {}", studentId, coursewareId, progress);

        Courseware courseware = coursewareMapper.selectById(coursewareId);
        if (courseware == null) {
            throw new BusinessException("课件不存在");
        }

        Chapter chapter = chapterMapper.selectById(courseware.getChapterId());
        Long courseId = chapter.getCourseId();

        LearningRecord record = learningRecordMapper.selectByStudentAndCourseware(studentId, coursewareId);

        if (record == null) {
            record = new LearningRecord();
            record.setStudentId(studentId);
            record.setCoursewareId(coursewareId);
            record.setCourseId(courseId);
            record.setChapterId(chapter.getId());
            record.setProgress(progress);
            record.setLastPosition(lastPosition);
            record.setStudyDuration(studyDuration);
            record.setStatus(LearningStatus.IN_PROGRESS.name());
            record.setStartedAt(LocalDateTime.now());

            if (progress >= 100) {
                record.setStatus(LearningStatus.COMPLETED.name());
                record.setCompletedAt(LocalDateTime.now());
                record.setProgress(100);
            }

            learningRecordMapper.insert(record);
        } else {
            record.setProgress(progress);
            record.setLastPosition(lastPosition);
            record.setStudyDuration(record.getStudyDuration() + studyDuration);

            if (progress >= 100) {
                record.setStatus(LearningStatus.COMPLETED.name());
                record.setCompletedAt(LocalDateTime.now());
                record.setProgress(100);
            }

            learningRecordMapper.updateById(record);
        }

        return record;
    }

    @Override
    public List<LearningRecord> getRecordsByCourse(Long studentId, Long courseId) {
        log.info("查询课程学习记录, studentId: {}, courseId: {}", studentId, courseId);
        return learningRecordMapper.selectByStudentAndCourse(studentId, courseId);
    }

    @Override
    public CourseProgressDTO getCourseProgress(Long studentId, Long courseId) {
        log.info("查询课程学习进度, studentId: {}, courseId: {}", studentId, courseId);

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        List<Chapter> chapters = chapterMapper.selectByCourseId(courseId);
        List<LearningRecord> records = learningRecordMapper.selectByStudentAndCourse(studentId, courseId);

        Map<Long, LearningRecord> recordMap = records.stream()
                .collect(Collectors.toMap(LearningRecord::getCoursewareId, r -> r, (a, b) -> a));

        List<ChapterProgressDTO> chapterProgressList = new ArrayList<>();
        int totalCoursewares = 0;
        int completedCoursewares = 0;

        for (Chapter chapter : chapters) {
            List<Courseware> coursewares = coursewareMapper.selectByChapterId(chapter.getId());
            int chapterTotal = coursewares.size();
            int chapterCompleted = 0;

            for (Courseware cw : coursewares) {
                LearningRecord rec = recordMap.get(cw.getId());
                if (rec != null && LearningStatus.COMPLETED.name().equals(rec.getStatus())) {
                    chapterCompleted++;
                }
            }

            totalCoursewares += chapterTotal;
            completedCoursewares += chapterCompleted;

            ChapterProgressDTO chapterProgress = ChapterProgressDTO.builder()
                    .chapterId(chapter.getId())
                    .chapterTitle(chapter.getTitle())
                    .totalCoursewares(chapterTotal)
                    .completedCoursewares(chapterCompleted)
                    .build();
            chapterProgressList.add(chapterProgress);
        }

        BigDecimal completionRate = BigDecimal.ZERO;
        if (totalCoursewares > 0) {
            completionRate = new BigDecimal(completedCoursewares)
                    .divide(new BigDecimal(totalCoursewares), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }

        CourseProgressDTO dto = CourseProgressDTO.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .totalCoursewares(totalCoursewares)
                .completedCoursewares(completedCoursewares)
                .completionRate(completionRate)
                .chapters(chapterProgressList)
                .build();
        return dto;
    }

    @Override
    public BigDecimal getCourseCompletionRate(Long studentId, Long courseId) {
        log.info("查询课程完成率, studentId: {}, courseId: {}", studentId, courseId);

        int total = coursewareMapper.countByCourseId(courseId);
        if (total == 0) {
            return BigDecimal.ZERO;
        }

        int completed = learningRecordMapper.countCompletedByStudentAndCourse(studentId, courseId);

        return new BigDecimal(completed)
                .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
    }
}
