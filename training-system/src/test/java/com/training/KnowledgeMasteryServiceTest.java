package com.training;

import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.impl.KnowledgeMasteryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Knowledge Mastery Service Tests")
class KnowledgeMasteryServiceTest {

    @Mock private KnowledgeMasteryMapper knowledgeMasteryMapper;
    @Mock private ChapterMapper chapterMapper;
    @Mock private CoursewareMapper coursewareMapper;
    @Mock private LearningRecordMapper learningRecordMapper;
    @Mock private AnswerDetailMapper answerDetailMapper;
    @Mock private AnswerSheetMapper answerSheetMapper;
    @Mock private QuestionMapper questionMapper;

    @InjectMocks
    private KnowledgeMasteryServiceImpl knowledgeMasteryService;

    @Nested
    @DisplayName("evaluateMastery")
    class EvaluateMasteryTests {

        @Test
        @DisplayName("should return MASTERED when all criteria are high scoring")
        void shouldReturnMasteredWhenHighScoring() {
            Chapter ch = Chapter.builder().id(1L).courseId(10L).title("Ch1").sortOrder(1).build();
            when(chapterMapper.selectList(any())).thenReturn(List.of(ch));

            Courseware cw = Courseware.builder().id(100L).chapterId(1L).durationSeconds(600).build();
            when(coursewareMapper.selectList(any())).thenReturn(List.of(cw));

            LearningRecord lr = LearningRecord.builder()
                    .coursewareId(100L).studyDurationSeconds(600).progressPercent(100.0).status("COMPLETED").build();
            when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr));

            // No wrong answers
            when(questionMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

            // No existing mastery record
            when(knowledgeMasteryMapper.selectOne(any())).thenReturn(null);

            List<KnowledgeMastery> results = knowledgeMasteryService.evaluateMastery(1L, 10L);

            assertEquals(1, results.size());
            assertEquals("MASTERED", results.get(0).getMasteryLevel());
            verify(knowledgeMasteryMapper).insert(any(KnowledgeMastery.class));
        }

        @Test
        @DisplayName("should return PARTIALLY_MASTERED when moderate performance")
        void shouldReturnPartiallyMastered() {
            Chapter ch = Chapter.builder().id(1L).courseId(10L).title("Ch1").sortOrder(1).build();
            when(chapterMapper.selectList(any())).thenReturn(List.of(ch));

            Courseware cw = Courseware.builder().id(100L).chapterId(1L).durationSeconds(1000).build();
            when(coursewareMapper.selectList(any())).thenReturn(List.of(cw));

            // 60% progress, partial study time
            LearningRecord lr = LearningRecord.builder()
                    .coursewareId(100L).studyDurationSeconds(500).progressPercent(60.0).status("IN_PROGRESS").build();
            when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr));

            // Some wrong answers
            Question q = Question.builder().id(200L).courseId(10L).chapterId(1L).status("ACTIVE").build();
            when(questionMapper.selectList(any())).thenReturn(List.of(q));

            AnswerSheet sheet = AnswerSheet.builder().id(300L).studentId(1L).status("SUBMITTED").tabSwitchCount(0).build();
            when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));

            AnswerDetail d1 = AnswerDetail.builder().answerSheetId(300L).questionId(200L).isCorrect(1).build();
            AnswerDetail d2 = AnswerDetail.builder().answerSheetId(300L).questionId(200L).isCorrect(0).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(d1, d2));

            when(knowledgeMasteryMapper.selectOne(any())).thenReturn(null);

            List<KnowledgeMastery> results = knowledgeMasteryService.evaluateMastery(1L, 10L);

            assertEquals(1, results.size());
            assertEquals("PARTIALLY_MASTERED", results.get(0).getMasteryLevel());
        }

        @Test
        @DisplayName("should return NOT_MASTERED when low performance")
        void shouldReturnNotMastered() {
            Chapter ch = Chapter.builder().id(1L).courseId(10L).title("Ch1").sortOrder(1).build();
            when(chapterMapper.selectList(any())).thenReturn(List.of(ch));

            Courseware cw = Courseware.builder().id(100L).chapterId(1L).durationSeconds(1000).build();
            when(coursewareMapper.selectList(any())).thenReturn(List.of(cw));

            // Very low progress
            LearningRecord lr = LearningRecord.builder()
                    .coursewareId(100L).studyDurationSeconds(100).progressPercent(10.0).status("IN_PROGRESS").build();
            when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr));

            // All wrong answers
            Question q = Question.builder().id(200L).courseId(10L).chapterId(1L).status("ACTIVE").build();
            when(questionMapper.selectList(any())).thenReturn(List.of(q));

            AnswerSheet sheet = AnswerSheet.builder().id(300L).studentId(1L).status("SUBMITTED").tabSwitchCount(0).build();
            when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));

            AnswerDetail d1 = AnswerDetail.builder().answerSheetId(300L).questionId(200L).isCorrect(0).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(d1));

            when(knowledgeMasteryMapper.selectOne(any())).thenReturn(null);

            List<KnowledgeMastery> results = knowledgeMasteryService.evaluateMastery(1L, 10L);

            assertEquals("NOT_MASTERED", results.get(0).getMasteryLevel());
        }

        @Test
        @DisplayName("should apply tab switch penalty reducing mastery score")
        void shouldApplyTabSwitchPenalty() {
            Chapter ch = Chapter.builder().id(1L).courseId(10L).title("Ch1").sortOrder(1).build();
            when(chapterMapper.selectList(any())).thenReturn(List.of(ch));

            Courseware cw = Courseware.builder().id(100L).chapterId(1L).durationSeconds(600).build();
            when(coursewareMapper.selectList(any())).thenReturn(List.of(cw));

            LearningRecord lr = LearningRecord.builder()
                    .coursewareId(100L).studyDurationSeconds(600).progressPercent(100.0).status("COMPLETED").build();
            when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr));

            when(questionMapper.selectList(any())).thenReturn(Collections.emptyList());

            // High tab switches: 6 * 0.05 = 0.3 penalty (max)
            AnswerSheet sheet = AnswerSheet.builder().id(300L).studentId(1L).status("SUBMITTED").tabSwitchCount(6).build();
            when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));

            when(knowledgeMasteryMapper.selectOne(any())).thenReturn(null);

            List<KnowledgeMastery> results = knowledgeMasteryService.evaluateMastery(1L, 10L);

            // Without penalty would be MASTERED (1.0), with -0.3 penalty = 0.7 -> PARTIALLY_MASTERED
            assertEquals("PARTIALLY_MASTERED", results.get(0).getMasteryLevel());
        }

        @Test
        @DisplayName("should handle chapter with no exam data")
        void shouldHandleNoExamData() {
            Chapter ch = Chapter.builder().id(1L).courseId(10L).title("Ch1").sortOrder(1).build();
            when(chapterMapper.selectList(any())).thenReturn(List.of(ch));

            Courseware cw = Courseware.builder().id(100L).chapterId(1L).durationSeconds(600).build();
            when(coursewareMapper.selectList(any())).thenReturn(List.of(cw));

            LearningRecord lr = LearningRecord.builder()
                    .coursewareId(100L).studyDurationSeconds(600).progressPercent(100.0).status("COMPLETED").build();
            when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr));

            // No questions, no sheets -> correctRatio defaults to 1.0
            when(questionMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(knowledgeMasteryMapper.selectOne(any())).thenReturn(null);

            List<KnowledgeMastery> results = knowledgeMasteryService.evaluateMastery(1L, 10L);

            assertEquals("MASTERED", results.get(0).getMasteryLevel());
        }
    }
}
