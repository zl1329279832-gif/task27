package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.entity.*;
import com.training.entity.dto.KnowledgeMasteryDTO;
import com.training.mapper.*;
import com.training.service.KnowledgeMasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeMasteryServiceImpl implements KnowledgeMasteryService {

    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final ChapterMapper chapterMapper;
    private final CoursewareMapper coursewareMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional
    public List<KnowledgeMastery> evaluateMastery(Long studentId, Long courseId) {
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getCourseId, courseId)
                        .orderByAsc(Chapter::getSortOrder));

        if (chapters.isEmpty()) return Collections.emptyList();

        // Get all coursewares for these chapters
        List<Long> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
        List<Courseware> allCoursewares = coursewareMapper.selectList(
                new LambdaQueryWrapper<Courseware>()
                        .in(Courseware::getChapterId, chapterIds));
        Map<Long, List<Courseware>> coursewareByChapter = allCoursewares.stream()
                .collect(Collectors.groupingBy(Courseware::getChapterId));

        // Get all learning records for the student in this course
        List<Long> coursewareIds = allCoursewares.stream().map(Courseware::getId).collect(Collectors.toList());
        Map<Long, LearningRecord> recordMap = Collections.emptyMap();
        if (!coursewareIds.isEmpty()) {
            List<LearningRecord> records = learningRecordMapper.selectList(
                    new LambdaQueryWrapper<LearningRecord>()
                            .eq(LearningRecord::getStudentId, studentId)
                            .in(LearningRecord::getCoursewareId, coursewareIds));
            recordMap = records.stream()
                    .collect(Collectors.toMap(LearningRecord::getCoursewareId, r -> r));
        }

        // Get questions linked to chapters for wrong answer analysis
        List<Question> chapterQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getCourseId, courseId)
                        .isNotNull(Question::getChapterId)
                        .ne(Question::getStatus, "DELETED"));
        Map<Long, List<Question>> questionsByChapter = chapterQuestions.stream()
                .collect(Collectors.groupingBy(Question::getChapterId));

        // Get student's answer details for wrong answer counting
        List<AnswerSheet> studentSheets = answerSheetMapper.selectList(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getStudentId, studentId)
                        .ne(AnswerSheet::getStatus, "IN_PROGRESS"));
        Map<Long, AnswerSheet> sheetMap = studentSheets.stream()
                .collect(Collectors.toMap(AnswerSheet::getId, s -> s));

        List<Long> sheetIds = studentSheets.stream().map(AnswerSheet::getId).collect(Collectors.toList());
        Map<Long, List<AnswerDetail>> detailsByQuestion = Collections.emptyMap();
        if (!sheetIds.isEmpty()) {
            List<AnswerDetail> allDetails = answerDetailMapper.selectList(
                    new LambdaQueryWrapper<AnswerDetail>()
                            .in(AnswerDetail::getAnswerSheetId, sheetIds));
            detailsByQuestion = allDetails.stream()
                    .collect(Collectors.groupingBy(AnswerDetail::getQuestionId));
        }

        // Calculate tab switch count from answer sheets for this course's exams
        int totalTabSwitches = studentSheets.stream()
                .mapToInt(s -> s.getTabSwitchCount() != null ? s.getTabSwitchCount() : 0)
                .sum();

        List<KnowledgeMastery> results = new ArrayList<>();

        for (Chapter chapter : chapters) {
            List<Courseware> chapterCws = coursewareByChapter.getOrDefault(chapter.getId(), Collections.emptyList());

            // Calculate study duration and expected duration
            int studyDuration = 0;
            int expectedDuration = 0;
            double progressSum = 0.0;
            int cwCount = chapterCws.size();

            for (Courseware cw : chapterCws) {
                expectedDuration += cw.getDurationSeconds() != null ? cw.getDurationSeconds() : 0;
                LearningRecord lr = recordMap.get(cw.getId());
                if (lr != null) {
                    studyDuration += lr.getStudyDurationSeconds() != null ? lr.getStudyDurationSeconds() : 0;
                    progressSum += lr.getProgressPercent() != null ? lr.getProgressPercent() : 0;
                }
            }

            double learningProgress = cwCount > 0 ? progressSum / cwCount : 0.0;

            // Calculate wrong answers for this chapter's questions
            List<Question> chapterQs = questionsByChapter.getOrDefault(chapter.getId(), Collections.emptyList());
            int wrongCount = 0;
            int totalCount = 0;
            for (Question q : chapterQs) {
                List<AnswerDetail> qDetails = detailsByQuestion.getOrDefault(q.getId(), Collections.emptyList());
                for (AnswerDetail d : qDetails) {
                    if (d.getIsCorrect() != null) {
                        totalCount++;
                        if (d.getIsCorrect() == 0) wrongCount++;
                    }
                }
            }

            // Calculate mastery score
            double durationRatio = expectedDuration > 0
                    ? Math.min(1.0, (double) studyDuration / expectedDuration)
                    : (studyDuration > 0 ? 1.0 : 0.0);
            double progressScore = learningProgress / 100.0;
            double correctRatio = totalCount > 0
                    ? (double) (totalCount - wrongCount) / totalCount
                    : 1.0;
            double tabPenalty = Math.min(totalTabSwitches * 0.05, 0.3);

            double masteryScore = (durationRatio * 0.3 + progressScore * 0.4 + correctRatio * 0.3) - tabPenalty;

            String masteryLevel;
            if (masteryScore >= 0.8) {
                masteryLevel = "MASTERED";
            } else if (masteryScore >= 0.5) {
                masteryLevel = "PARTIALLY_MASTERED";
            } else {
                masteryLevel = "NOT_MASTERED";
            }

            // Upsert
            KnowledgeMastery existing = knowledgeMasteryMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeMastery>()
                            .eq(KnowledgeMastery::getStudentId, studentId)
                            .eq(KnowledgeMastery::getChapterId, chapter.getId()));

            if (existing != null) {
                existing.setMasteryLevel(masteryLevel);
                existing.setLearningProgress(learningProgress);
                existing.setStudyDurationSeconds(studyDuration);
                existing.setExpectedDurationSeconds(expectedDuration);
                existing.setWrongAnswerCount(wrongCount);
                existing.setTotalAnswerCount(totalCount);
                existing.setTabSwitchCount(totalTabSwitches);
                existing.setLastEvaluatedAt(LocalDateTime.now());
                knowledgeMasteryMapper.updateById(existing);
                results.add(existing);
            } else {
                KnowledgeMastery mastery = KnowledgeMastery.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .chapterId(chapter.getId())
                        .masteryLevel(masteryLevel)
                        .learningProgress(learningProgress)
                        .studyDurationSeconds(studyDuration)
                        .expectedDurationSeconds(expectedDuration)
                        .wrongAnswerCount(wrongCount)
                        .totalAnswerCount(totalCount)
                        .tabSwitchCount(totalTabSwitches)
                        .lastEvaluatedAt(LocalDateTime.now())
                        .build();
                knowledgeMasteryMapper.insert(mastery);
                results.add(mastery);
            }
        }

        return results;
    }

    @Override
    public KnowledgeMastery getChapterMastery(Long studentId, Long chapterId) {
        return knowledgeMasteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getStudentId, studentId)
                        .eq(KnowledgeMastery::getChapterId, chapterId));
    }

    @Override
    public List<KnowledgeMasteryDTO> getMasteryByCourse(Long studentId, Long courseId) {
        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getStudentId, studentId)
                        .eq(KnowledgeMastery::getCourseId, courseId));

        if (masteries.isEmpty()) return Collections.emptyList();

        List<Long> chapterIds = masteries.stream()
                .map(KnowledgeMastery::getChapterId).collect(Collectors.toList());
        Map<Long, Chapter> chapterMap = chapterMapper.selectBatchIds(chapterIds).stream()
                .collect(Collectors.toMap(Chapter::getId, c -> c));

        return masteries.stream().map(m -> {
            Chapter ch = chapterMap.get(m.getChapterId());
            return KnowledgeMasteryDTO.builder()
                    .chapterId(m.getChapterId())
                    .chapterTitle(ch != null ? ch.getTitle() : null)
                    .masteryLevel(m.getMasteryLevel())
                    .learningProgress(m.getLearningProgress())
                    .wrongAnswerCount(m.getWrongAnswerCount())
                    .totalAnswerCount(m.getTotalAnswerCount())
                    .studyDurationSeconds(m.getStudyDurationSeconds())
                    .expectedDurationSeconds(m.getExpectedDurationSeconds())
                    .build();
        }).collect(Collectors.toList());
    }
}
