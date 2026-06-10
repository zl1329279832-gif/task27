package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.AnswerSubmitRequest;
import com.training.entity.dto.ExamStartResponse;
import com.training.mapper.*;
import com.training.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exam Service Tests")
class ExamServiceTest {

    @Mock private ExamMapper examMapper;
    @Mock private ExamQuestionMapper examQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private AnswerSheetMapper answerSheetMapper;
    @Mock private AnswerDetailMapper answerDetailMapper;
    @Mock private GradeMapper gradeMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ExamServiceImpl examService;

    private Exam publishedExam;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        publishedExam = Exam.builder()
                .id(1L)
                .courseId(10L)
                .title("Java Basic Exam")
                .durationMinutes(60)
                .totalScore(100)
                .passScore(60)
                .questionCount(0)
                .randomize(0)
                .maxAttempts(3)
                .antiCheatEnabled(1)
                .maxTabSwitches(3)
                .status("PUBLISHED")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .build();
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private AnswerSheet.QuestionSnapshot buildSnapshot(Long qId, Long eqId, String content,
                                                        String type, String correctAns,
                                                        int score, int sortOrder) {
        return AnswerSheet.QuestionSnapshot.builder()
                .questionId(qId)
                .examQuestionId(eqId)
                .content(content)
                .questionType(type)
                .correctAnswer(correctAns)
                .score(score)
                .sortOrder(sortOrder)
                .build();
    }

    private AnswerSubmitRequest buildSubmitRequest(Long sheetId, Long... questionIds) {
        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(sheetId);
        if (questionIds != null && questionIds.length > 0) {
            var answers = new java.util.ArrayList<AnswerSubmitRequest.AnswerItem>();
            for (Long qId : questionIds) {
                AnswerSubmitRequest.AnswerItem item = new AnswerSubmitRequest.AnswerItem();
                item.setQuestionId(qId);
                item.setAnswer("A");
                answers.add(item);
            }
            req.setAnswers(answers);
        } else {
            req.setAnswers(Collections.emptyList());
        }
        return req;
    }

    // ========================================================================
    // startExam tests
    // ========================================================================

    @Nested
    @DisplayName("startExam")
    class StartExamTests {

        @Test
        @DisplayName("should create answer sheet with frozen question snapshot")
        void shouldCreateAnswerSheetWithSnapshot() {
            Question q = Question.builder()
                    .id(100L)
                    .content("What is Java?")
                    .questionType("SINGLE_CHOICE")
                    .options(Arrays.asList("Compiled", "Interpreted", "Assembly", "Machine"))
                    .correctAnswer("A")
                    .score(20)
                    .status("ACTIVE")
                    .build();

            ExamQuestion eq = ExamQuestion.builder()
                    .id(1L).examId(1L).questionId(100L).sortOrder(1).build();

            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerSheetMapper.selectCount(any())).thenReturn(0L);
            when(answerSheetMapper.selectOne(any())).thenReturn(null);
            when(examQuestionMapper.selectList(any())).thenReturn(List.of(eq));
            when(questionMapper.selectById(100L)).thenReturn(q);

            ExamStartResponse response = examService.startExam(1L, 1L);

            assertNotNull(response);
            assertFalse(response.getResumed());
            assertEquals(1, response.getAttemptNo());
            assertEquals(3600, response.getRemainingSeconds());
            assertEquals(1, response.getQuestions().size());
            assertEquals("What is Java?", response.getQuestions().get(0).getContent());
            assertTrue(response.getExistingAnswers().isEmpty());

            // Verify snapshot is saved to answer sheet
            ArgumentCaptor<AnswerSheet> sheetCaptor = ArgumentCaptor.forClass(AnswerSheet.class);
            verify(answerSheetMapper, atLeast(1)).updateById(sheetCaptor.capture());
            AnswerSheet updatedSheet = sheetCaptor.getValue();
            assertNotNull(updatedSheet.getQuestionSnapshot());
            assertEquals(1, updatedSheet.getQuestionSnapshot().size());
            assertEquals("What is Java?", updatedSheet.getQuestionSnapshot().get(0).getContent());
            assertEquals("A", updatedSheet.getQuestionSnapshot().get(0).getCorrectAnswer());
            assertEquals(20, updatedSheet.getQuestionSnapshot().get(0).getScore());

            // Verify AnswerDetail has frozen correctAnswer and snapshotScore
            ArgumentCaptor<AnswerDetail> detailCaptor = ArgumentCaptor.forClass(AnswerDetail.class);
            verify(answerDetailMapper).insert(detailCaptor.capture());
            AnswerDetail savedDetail = detailCaptor.getValue();
            assertEquals("A", savedDetail.getCorrectAnswer());
            assertEquals(20, savedDetail.getSnapshotScore());

            verify(answerSheetMapper).insert(any(AnswerSheet.class));
            verify(valueOperations).set(eq("exam:timeout:" + response.getAnswerSheetId()),
                    eq("1"), eq(3600L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("should throw BusinessException when max attempts exceeded")
        void shouldThrowWhenMaxAttemptsExceeded() {
            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerSheetMapper.selectCount(any())).thenReturn(3L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> examService.startExam(1L, 1L));
            assertTrue(ex.getMessage().contains("最大考试次数"));
        }

        @Test
        @DisplayName("should throw BusinessException when exam is not published")
        void shouldThrowWhenExamNotPublished() {
            publishedExam.setStatus("DRAFT");
            when(examMapper.selectById(1L)).thenReturn(publishedExam);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> examService.startExam(1L, 1L));
            assertTrue(ex.getMessage().contains("未发布"));
        }
    }

    // ========================================================================
    // Breakpoint resume tests (critical: must use frozen snapshot)
    // ========================================================================

    @Nested
    @DisplayName("Breakpoint Resume (断点续考)")
    class BreakpointResumeTests {

        @Test
        @DisplayName("resume should use frozen snapshot even when question is deleted from bank")
        void resumeShouldUseSnapshotWhenQuestionDeleted() {
            // Simulate: student started exam, then admin deleted question from bank
            AnswerSheet.QuestionSnapshot frozenSnapshot = buildSnapshot(
                    100L, 1L, "Original question content",
                    "SINGLE_CHOICE", "A", 20, 1);

            AnswerSheet existingSheet = AnswerSheet.builder()
                    .id(5L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").remainingSeconds(1800)
                    .startTime(LocalDateTime.now().minusMinutes(30))
                    .tabSwitchCount(0)
                    .questionSnapshot(List.of(frozenSnapshot))
                    .build();

            AnswerDetail detail = AnswerDetail.builder()
                    .id(10L).answerSheetId(5L).questionId(100L).examQuestionId(1L)
                    .studentAnswer("A")
                    .correctAnswer("A").snapshotScore(20)
                    .build();

            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerSheetMapper.selectCount(any())).thenReturn(0L);
            when(answerSheetMapper.selectOne(any())).thenReturn(existingSheet);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(1500L);

            ExamStartResponse response = examService.startExam(1L, 1L);

            assertTrue(response.getResumed());
            assertEquals(5L, response.getAnswerSheetId());
            assertEquals(1500, response.getRemainingSeconds());

            // CRITICAL: question content comes from SNAPSHOT, not live table
            assertEquals(1, response.getQuestions().size());
            assertEquals("Original question content", response.getQuestions().get(0).getContent());
            assertEquals(20, response.getQuestions().get(0).getScore());

            // Verify we did NOT read from the question table
            verify(questionMapper, never()).selectById(anyLong());

            // Verify existing answers preserved
            assertEquals(1, response.getExistingAnswers().size());
            assertEquals("A", response.getExistingAnswers().get(0).getStudentAnswer());

            // No new answer sheet or detail created
            verify(answerSheetMapper, never()).insert(any());
            verify(answerDetailMapper, never()).insert(any());
        }

        @Test
        @DisplayName("resume should use frozen snapshot even when question content was modified")
        void resumeShouldUseSnapshotWhenQuestionModified() {
            // Simulate: admin changed question content after student started exam
            AnswerSheet.QuestionSnapshot frozenSnapshot = buildSnapshot(
                    100L, 1L, "Original version of question",
                    "SINGLE_CHOICE", "B", 25, 1);

            AnswerSheet existingSheet = AnswerSheet.builder()
                    .id(5L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").remainingSeconds(1800)
                    .startTime(LocalDateTime.now().minusMinutes(30))
                    .tabSwitchCount(0)
                    .questionSnapshot(List.of(frozenSnapshot))
                    .build();

            AnswerDetail detail = AnswerDetail.builder()
                    .id(10L).answerSheetId(5L).questionId(100L).examQuestionId(1L)
                    .correctAnswer("B").snapshotScore(25)
                    .build();

            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerSheetMapper.selectCount(any())).thenReturn(0L);
            when(answerSheetMapper.selectOne(any())).thenReturn(existingSheet);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(1500L);

            ExamStartResponse response = examService.startExam(1L, 1L);

            assertTrue(response.getResumed());
            // Must show FROZEN content, NOT modified content
            assertEquals("Original version of question", response.getQuestions().get(0).getContent());
            assertEquals(25, response.getQuestions().get(0).getScore());

            // Never read from live question table
            verify(questionMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("resume should compute remaining time from DB when Redis TTL expired")
        void resumeShouldFallbackToDbTimeWhenRedisExpired() {
            AnswerSheet.QuestionSnapshot snap = buildSnapshot(
                    100L, 1L, "Q1", "SINGLE_CHOICE", "A", 20, 1);

            AnswerSheet existingSheet = AnswerSheet.builder()
                    .id(5L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").remainingSeconds(3600)
                    .startTime(LocalDateTime.now().minusMinutes(30))
                    .tabSwitchCount(0)
                    .questionSnapshot(List.of(snap))
                    .build();

            AnswerDetail detail = AnswerDetail.builder()
                    .id(10L).answerSheetId(5L).questionId(100L).examQuestionId(1L)
                    .correctAnswer("A").snapshotScore(20)
                    .build();

            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerSheetMapper.selectCount(any())).thenReturn(0L);
            when(answerSheetMapper.selectOne(any())).thenReturn(existingSheet);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
            // Redis key has expired (returns negative TTL)
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-2L);

            ExamStartResponse response = examService.startExam(1L, 1L);

            assertTrue(response.getResumed());
            // 60 minutes total - 30 minutes elapsed = ~30 minutes remaining
            assertTrue(response.getRemainingSeconds() > 0);
            assertTrue(response.getRemainingSeconds() <= 1860); // ~31 min with test timing tolerance
        }
    }

    // ========================================================================
    // submitExam tests
    // ========================================================================

    @Nested
    @DisplayName("submitExam")
    class SubmitExamTests {

        @Test
        @DisplayName("should auto-grade using frozen correctAnswer, not live question data")
        void shouldAutoGradeUsingFrozenData() {
            AnswerSheet.QuestionSnapshot snap = buildSnapshot(
                    101L, 201L, "Q1", "SINGLE_CHOICE", "A", 20, 1);

            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").tabSwitchCount(0)
                    .questionSnapshot(List.of(snap))
                    .build();

            AnswerDetail d1 = AnswerDetail.builder()
                    .id(1L).answerSheetId(1L).questionId(101L).examQuestionId(201L)
                    .studentAnswer("A")
                    .correctAnswer("A")  // frozen correct answer
                    .snapshotScore(20)   // frozen score
                    .build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(null);
            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(d1));

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            AnswerSheet result = examService.submitExam(req, 1L);

            assertEquals("SUBMITTED", result.getStatus());
            // Should not read from question table - uses frozen data
            verify(questionMapper, never()).selectById(anyLong());

            // Verify grading used frozen data
            ArgumentCaptor<AnswerDetail> captor = ArgumentCaptor.forClass(AnswerDetail.class);
            verify(answerDetailMapper).updateById(captor.capture());
            AnswerDetail graded = captor.getValue();
            assertEquals(1, graded.getIsCorrect());
            assertEquals(20.0, graded.getScoreEarned());
        }

        @Test
        @DisplayName("should throw on duplicate submission (Redis idempotent check)")
        void shouldThrowOnDuplicateSubmission() {
            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> examService.submitExam(req, 1L));
            assertTrue(ex.getMessage().contains("重复提交"));

            verify(examMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("should reject submission when answer sheet already submitted")
        void shouldRejectWhenAlreadySubmitted() {
            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("SUBMITTED").build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> examService.submitExam(req, 1L));
            assertTrue(ex.getMessage().contains("已提交"));
        }

        @Test
        @DisplayName("should reject submission when answer sheet timed out")
        void shouldRejectWhenTimedOut() {
            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("TIMED_OUT").build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> examService.submitExam(req, 1L));
            assertTrue(ex.getMessage().contains("已提交或已超时"));
        }

        @Test
        @DisplayName("should give full score for deleted question using snapshot data")
        void shouldGiveFullScoreForDeletedQuestionWithSnapshot() {
            // Question was in snapshot at start, then deleted from bank
            // Since snapshot exists, grading uses snapshot data normally
            AnswerSheet.QuestionSnapshot snap = buildSnapshot(
                    100L, 200L, "Deleted Q", "SINGLE_CHOICE", "A", 10, 1);

            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").tabSwitchCount(0)
                    .questionSnapshot(List.of(snap))
                    .build();

            // AnswerDetail with frozen data from snapshot
            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                    .studentAnswer("wrong")
                    .correctAnswer("A")
                    .snapshotScore(10)
                    .build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(null);
            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            AnswerSheet result = examService.submitExam(req, 1L);

            assertEquals("SUBMITTED", result.getStatus());

            ArgumentCaptor<AnswerDetail> captor = ArgumentCaptor.forClass(AnswerDetail.class);
            verify(answerDetailMapper).updateById(captor.capture());
            AnswerDetail graded = captor.getValue();
            // Student answered "wrong" but correct was "A" -> incorrect
            assertEquals(0, graded.getIsCorrect());
            assertEquals(0.0, graded.getScoreEarned());

            // Should not read from question table
            verify(questionMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("should auto-grade MULTI_CHOICE correctly using frozen data")
        void shouldAutoGradeMultiChoiceWithFrozenData() {
            AnswerSheet.QuestionSnapshot snap = buildSnapshot(
                    102L, 202L, "Multi Q", "MULTI_CHOICE", "A,B,C", 30, 1);

            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").tabSwitchCount(0)
                    .questionSnapshot(List.of(snap))
                    .build();

            // Student answered in different order
            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(1L).questionId(102L).examQuestionId(202L)
                    .studentAnswer("C,A,B")
                    .correctAnswer("A,B,C")
                    .snapshotScore(30)
                    .build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(null);
            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            AnswerSheet result = examService.submitExam(req, 1L);

            ArgumentCaptor<AnswerDetail> captor = ArgumentCaptor.forClass(AnswerDetail.class);
            verify(answerDetailMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getIsCorrect());
            assertEquals(30.0, captor.getValue().getScoreEarned());
        }

        @Test
        @DisplayName("should clean up idempotent key on grading failure to allow retry")
        void shouldCleanIdempotentKeyOnFailure() {
            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").tabSwitchCount(0)
                    .build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(null);
            // Simulate exception during grading
            when(examMapper.selectById(1L)).thenThrow(new RuntimeException("DB error"));

            AnswerSubmitRequest req = buildSubmitRequest(1L);

            assertThrows(RuntimeException.class, () -> examService.submitExam(req, 1L));

            // Idempotent key should be cleaned up on failure
            verify(redisTemplate).delete(contains("submit:"));
        }
    }

    // ========================================================================
    // Timeout auto-submit tests
    // ========================================================================

    @Nested
    @DisplayName("Timeout Auto-Submit (超时自动交卷)")
    class TimeoutTests {

        @Test
        @DisplayName("handleTimeout should auto-submit and use frozen data for grading")
        void handleTimeoutShouldAutoSubmitWithFrozenData() {
            AnswerSheet.QuestionSnapshot snap = buildSnapshot(
                    100L, 200L, "Q1", "SINGLE_CHOICE", "A", 20, 1);

            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("IN_PROGRESS").tabSwitchCount(0)
                    .questionSnapshot(List.of(snap))
                    .build();

            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                    .studentAnswer("A")
                    .correctAnswer("A").snapshotScore(20)
                    .build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

            examService.handleTimeout(1L);

            // Verify status changed to TIMED_OUT
            verify(answerSheetMapper).updateById(argThat(s ->
                    "TIMED_OUT".equals(s.getStatus())));

            // Redis timeout key should be cleaned
            verify(redisTemplate).delete("exam:timeout:1");

            // Grade record should be created (all objective questions graded)
            verify(gradeMapper).insert(any(Grade.class));
        }

        @Test
        @DisplayName("handleTimeout should be no-op for already submitted sheet")
        void handleTimeoutShouldNoOpForAlreadySubmitted() {
            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L).attemptNo(1)
                    .status("SUBMITTED").build();

            when(answerSheetMapper.selectById(1L)).thenReturn(sheet);

            examService.handleTimeout(1L);

            // Should not proceed with grading
            verify(examMapper, never()).selectById(anyLong());
            verify(answerDetailMapper, never()).selectList(any());
        }
    }

    // ========================================================================
    // Tab switch anti-cheat tests
    // ========================================================================

    @Nested
    @DisplayName("Tab Switch Anti-Cheat")
    class TabSwitchTests {

        @Test
        @DisplayName("should auto-submit when tab switches exceed anti-cheat limit")
        void shouldAutoSubmitWhenExceedsLimit() {
            AnswerSheet.QuestionSnapshot snap = buildSnapshot(
                    100L, 200L, "Q1", "SINGLE_CHOICE", "A", 20, 1);

            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L)
                    .status("IN_PROGRESS").tabSwitchCount(3)
                    .questionSnapshot(List.of(snap))
                    .build();

            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                    .correctAnswer("A").snapshotScore(20)
                    .build();

            when(answerSheetMapper.selectOne(any())).thenReturn(sheet);
            when(examMapper.selectById(1L)).thenReturn(publishedExam);
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

            examService.reportTabSwitch(1L, 1L);

            verify(answerSheetMapper, atLeastOnce()).updateById(argThat(s ->
                    "AUTO_SUBMITTED".equals(s.getStatus())));
            verify(redisTemplate).delete("exam:timeout:1");
        }

        @Test
        @DisplayName("should only increment count when under the limit")
        void shouldOnlyIncrementWhenUnderLimit() {
            AnswerSheet sheet = AnswerSheet.builder()
                    .id(1L).examId(1L).studentId(1L)
                    .status("IN_PROGRESS").tabSwitchCount(1).build();

            when(answerSheetMapper.selectOne(any())).thenReturn(sheet);
            when(examMapper.selectById(1L)).thenReturn(publishedExam);

            examService.reportTabSwitch(1L, 1L);

            verify(answerSheetMapper, times(1)).updateById(any(AnswerSheet.class));
            verify(answerDetailMapper, never()).selectList(any());
        }
    }
}
