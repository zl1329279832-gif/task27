package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.AnswerSubmitRequest;
import com.training.entity.dto.ExamStartResponse;
import com.training.mapper.*;
import com.training.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    // startExam tests
    // ========================================================================

    @Test
    @DisplayName("startExam: should create answer sheet and snapshot question data")
    void startExam_shouldCreateAnswerSheetAndSnapshotQuestions() {
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

        // Verify snapshot fields are populated in AnswerDetail
        ArgumentCaptor<AnswerDetail> detailCaptor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper).insert(detailCaptor.capture());
        AnswerDetail captured = detailCaptor.getValue();
        assertEquals("What is Java?", captured.getSnapshotContent());
        assertEquals("SINGLE_CHOICE", captured.getSnapshotQuestionType());
        assertEquals("A", captured.getSnapshotCorrectAnswer());
        assertEquals(20, captured.getSnapshotScore());
        assertEquals(Arrays.asList("Compiled", "Interpreted", "Assembly", "Machine"),
                captured.getSnapshotOptions());

        verify(answerSheetMapper).insert(any(AnswerSheet.class));
    }

    @Test
    @DisplayName("startExam: snapshot should use scoreOverride from ExamQuestion when present")
    void startExam_shouldUseScoreOverrideInSnapshot() {
        Question q = Question.builder()
                .id(100L).content("Q1").questionType("SINGLE_CHOICE")
                .options(List.of("A", "B")).correctAnswer("A").score(10).status("ACTIVE").build();

        ExamQuestion eq = ExamQuestion.builder()
                .id(1L).examId(1L).questionId(100L).sortOrder(1)
                .scoreOverride(25) // Override score
                .build();

        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerSheetMapper.selectCount(any())).thenReturn(0L);
        when(answerSheetMapper.selectOne(any())).thenReturn(null);
        when(examQuestionMapper.selectList(any())).thenReturn(List.of(eq));
        when(questionMapper.selectById(100L)).thenReturn(q);

        examService.startExam(1L, 1L);

        ArgumentCaptor<AnswerDetail> captor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper).insert(captor.capture());
        assertEquals(25, captor.getValue().getSnapshotScore());
    }

    @Test
    @DisplayName("startExam: should resume using snapshot data, not live question table")
    void startExam_shouldResumeUsingSnapshotData() {
        AnswerSheet existingSheet = AnswerSheet.builder()
                .id(5L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").remainingSeconds(1800)
                .startTime(LocalDateTime.now().minusMinutes(30))
                .tabSwitchCount(0).build();

        // Snapshot preserves original question content
        AnswerDetail detail = AnswerDetail.builder()
                .id(10L).answerSheetId(5L).questionId(100L).examQuestionId(1L)
                .studentAnswer("A")
                .snapshotContent("Original question text")
                .snapshotQuestionType("SINGLE_CHOICE")
                .snapshotOptions(Arrays.asList("A", "B", "C"))
                .snapshotCorrectAnswer("A")
                .snapshotScore(20)
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
        // Verify resume uses snapshot content, not live question
        assertEquals("Original question text", response.getQuestions().get(0).getContent());
        assertEquals("SINGLE_CHOICE", response.getQuestions().get(0).getQuestionType());
        assertEquals(Arrays.asList("A", "B", "C"), response.getQuestions().get(0).getOptions());
        assertEquals(20, response.getQuestions().get(0).getScore());
        assertEquals(1, response.getExistingAnswers().size());
        assertEquals("A", response.getExistingAnswers().get(0).getStudentAnswer());

        // Must NOT read from live question table during resume
        verify(questionMapper, never()).selectById(anyLong());
        verify(answerSheetMapper, never()).insert(any());
    }

    @Test
    @DisplayName("startExam: resume should show snapshot even after question is deleted")
    void startExam_resumeShouldShowSnapshotAfterQuestionDeleted() {
        AnswerSheet existingSheet = AnswerSheet.builder()
                .id(5L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").remainingSeconds(3600)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .tabSwitchCount(0).build();

        // Question was deleted after exam started, but snapshot preserves original data
        AnswerDetail detail = AnswerDetail.builder()
                .id(10L).answerSheetId(5L).questionId(100L).examQuestionId(1L)
                .snapshotContent("What is polymorphism?")
                .snapshotQuestionType("SINGLE_CHOICE")
                .snapshotOptions(Arrays.asList("Inheritance", "Encapsulation", "Multiple forms", "Abstraction"))
                .snapshotCorrectAnswer("C")
                .snapshotScore(15)
                .build();

        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerSheetMapper.selectCount(any())).thenReturn(0L);
        when(answerSheetMapper.selectOne(any())).thenReturn(existingSheet);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(3000L);

        ExamStartResponse response = examService.startExam(1L, 1L);

        // Even though question was deleted, snapshot data is available
        assertEquals("What is polymorphism?", response.getQuestions().get(0).getContent());
        assertEquals(15, response.getQuestions().get(0).getScore());
        verify(questionMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("startExam: should throw BusinessException when max attempts exceeded")
    void startExam_shouldThrowWhenMaxAttemptsExceeded() {
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerSheetMapper.selectCount(any())).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examService.startExam(1L, 1L));
        assertTrue(ex.getMessage().contains("最大考试次数"));
    }

    // ========================================================================
    // submitExam tests
    // ========================================================================

    @Test
    @DisplayName("submitExam: should auto-grade using snapshot data, not live question table")
    void submitExam_shouldAutoGradeUsingSnapshotData() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").tabSwitchCount(0).build();

        // Answer details with snapshot data and pre-populated student answers
        AnswerDetail d1 = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(101L).examQuestionId(201L)
                .studentAnswer("A")
                .snapshotQuestionType("SINGLE_CHOICE").snapshotCorrectAnswer("A").snapshotScore(20)
                .build();
        AnswerDetail d2 = AnswerDetail.builder()
                .id(2L).answerSheetId(1L).questionId(102L).examQuestionId(202L)
                .studentAnswer("A,B")
                .snapshotQuestionType("MULTI_CHOICE").snapshotCorrectAnswer("A,B").snapshotScore(20)
                .build();
        AnswerDetail d3 = AnswerDetail.builder()
                .id(3L).answerSheetId(1L).questionId(103L).examQuestionId(203L)
                .studentAnswer("True")
                .snapshotQuestionType("TRUE_FALSE").snapshotCorrectAnswer("True").snapshotScore(20)
                .build();
        AnswerDetail d4 = AnswerDetail.builder()
                .id(4L).answerSheetId(1L).questionId(104L).examQuestionId(204L)
                .studentAnswer("essay answer")
                .snapshotQuestionType("SHORT_ANSWER").snapshotCorrectAnswer("N/A").snapshotScore(40)
                .build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(Arrays.asList(d1, d2, d3, d4));

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        AnswerSheet result = examService.submitExam(req, 1L);

        assertEquals("SUBMITTED", result.getStatus());
        assertEquals(0, result.getRemainingSeconds());

        // Grading must NOT read from live question table
        verify(questionMapper, never()).selectById(anyLong());
        verify(examQuestionMapper, never()).selectById(anyLong());

        ArgumentCaptor<AnswerDetail> detailCaptor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper, times(4)).updateById(detailCaptor.capture());
        List<AnswerDetail> updated = detailCaptor.getAllValues();

        assertEquals(1, updated.get(0).getIsCorrect());   // SINGLE_CHOICE correct
        assertEquals(20.0, updated.get(0).getScoreEarned());
        assertEquals(1, updated.get(1).getIsCorrect());   // MULTI_CHOICE correct
        assertEquals(1, updated.get(2).getIsCorrect());   // TRUE_FALSE correct
        assertEquals("待人工批改", updated.get(3).getGradingNote()); // SHORT_ANSWER

        // SHORT_ANSWER pending → no Grade record
        verify(gradeMapper, never()).insert(any(Grade.class));
    }

    @Test
    @DisplayName("submitExam: snapshot grading is immune to post-start question modifications")
    void submitExam_snapshotImmutableToQuestionModifications() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").tabSwitchCount(0).build();

        // Snapshot says correct answer is "A", even if live question was changed to "B"
        AnswerDetail detail = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                .studentAnswer("A")
                .snapshotQuestionType("SINGLE_CHOICE")
                .snapshotCorrectAnswer("A") // Original correct answer at exam start
                .snapshotScore(10)
                .snapshotContent("Original question")
                .build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        AnswerSheet result = examService.submitExam(req, 1L);

        ArgumentCaptor<AnswerDetail> captor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper).updateById(captor.capture());

        // Student answered "A", snapshot says "A" is correct → marked correct
        // Even though the live question may now say "B" is correct
        assertEquals(1, captor.getValue().getIsCorrect());
        assertEquals(10.0, captor.getValue().getScoreEarned());

        // Grading never touched the live question table
        verify(questionMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("submitExam: repeated submission returns existing sheet without re-scoring")
    void submitExam_repeatedSubmissionReturnsExistingWithoutReScoring() {
        // Sheet is already SUBMITTED (previous submission succeeded)
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("SUBMITTED").score(80.0).pass(1)
                .submitTime(LocalDateTime.now().minusMinutes(5))
                .build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        // Should return existing sheet, not throw or re-grade
        AnswerSheet result = examService.submitExam(req, 1L);

        assertEquals("SUBMITTED", result.getStatus());
        assertEquals(80.0, result.getScore());
        assertEquals(1, result.getPass());

        // No grading should happen
        verify(answerDetailMapper, never()).selectList(any());
        verify(answerDetailMapper, never()).updateById(any());
        verify(gradeMapper, never()).insert(any());
        // Redis idempotency check should not be reached
        verify(valueOperations, never()).setIfAbsent(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("submitExam: concurrent duplicate returns sheet without re-scoring (Redis guard)")
    void submitExam_concurrentDuplicateReturnsSheetWithoutReScoring() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        // Redis setIfAbsent returns false → concurrent duplicate within 30s window
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        // Should return existing sheet without re-grading
        AnswerSheet result = examService.submitExam(req, 1L);

        assertNotNull(result);
        verify(examMapper, never()).selectById(anyLong());
        verify(answerDetailMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("submitExam: timed-out sheet cannot be re-submitted")
    void submitExam_timedOutSheetCannotBeReSubmitted() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("TIMED_OUT").score(30.0).pass(0)
                .submitTime(LocalDateTime.now().minusMinutes(10))
                .build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        AnswerSheet result = examService.submitExam(req, 1L);

        assertEquals("TIMED_OUT", result.getStatus());
        assertEquals(30.0, result.getScore());
        // No re-grading
        verify(answerDetailMapper, never()).selectList(any());
    }

    // ========================================================================
    // handleTimeout tests
    // ========================================================================

    @Test
    @DisplayName("handleTimeout: should auto-grade with TIMED_OUT status using snapshot data")
    void handleTimeout_shouldAutoGradeWithTimedOutStatus() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").tabSwitchCount(0)
                .startTime(LocalDateTime.now().minusMinutes(65))
                .remainingSeconds(3600)
                .build();

        AnswerDetail detail = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                .studentAnswer("B")
                .snapshotQuestionType("SINGLE_CHOICE")
                .snapshotCorrectAnswer("A")
                .snapshotScore(20)
                .build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

        examService.handleTimeout(1L);

        // Verify sheet status set to TIMED_OUT
        ArgumentCaptor<AnswerSheet> sheetCaptor = ArgumentCaptor.forClass(AnswerSheet.class);
        verify(answerSheetMapper).updateById(sheetCaptor.capture());
        assertEquals("TIMED_OUT", sheetCaptor.getValue().getStatus());
        assertEquals(0, sheetCaptor.getValue().getRemainingSeconds());

        // Verify grading used snapshot (wrong answer → 0 score)
        ArgumentCaptor<AnswerDetail> detailCaptor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper).updateById(detailCaptor.capture());
        assertEquals(0, detailCaptor.getValue().getIsCorrect());
        assertEquals(0.0, detailCaptor.getValue().getScoreEarned());

        verify(questionMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("handleTimeout: should skip already submitted sheets")
    void handleTimeout_shouldSkipAlreadySubmittedSheets() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("SUBMITTED").build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);

        examService.handleTimeout(1L);

        verify(answerDetailMapper, never()).selectList(any());
        verify(answerSheetMapper, never()).updateById(any());
    }

    // ========================================================================
    // reportTabSwitch tests
    // ========================================================================

    @Test
    @DisplayName("reportTabSwitch: should auto-submit using snapshot when exceeds limit")
    void reportTabSwitch_shouldAutoSubmitWithSnapshotWhenExceedsLimit() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS").tabSwitchCount(3).build();

        AnswerDetail detail = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                .snapshotQuestionType("SINGLE_CHOICE")
                .snapshotCorrectAnswer("A")
                .snapshotScore(20)
                .build();

        when(answerSheetMapper.selectOne(any())).thenReturn(sheet);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

        examService.reportTabSwitch(1L, 1L);

        verify(answerSheetMapper, atLeastOnce()).updateById(argThat(s ->
                "AUTO_SUBMITTED".equals(s.getStatus())));
        verify(redisTemplate).delete("exam:timeout:1");
        // Grading uses snapshot, not live question
        verify(questionMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("reportTabSwitch: should only increment count when under the limit")
    void reportTabSwitch_shouldOnlyIncrementWhenUnderLimit() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS").tabSwitchCount(1).build();

        when(answerSheetMapper.selectOne(any())).thenReturn(sheet);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);

        examService.reportTabSwitch(1L, 1L);

        verify(answerSheetMapper, times(1)).updateById(any(AnswerSheet.class));
        verify(answerSheetMapper, never()).updateById(argThat(s ->
                "AUTO_SUBMITTED".equals(s.getStatus())));
        verify(answerDetailMapper, never()).selectList(any());
    }
}
