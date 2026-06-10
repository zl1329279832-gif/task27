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
    @DisplayName("startExam: should create answer sheet and return questions for new attempt")
    void startExam_shouldCreateAnswerSheetAndReturnQuestions() {
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

        verify(answerSheetMapper).insert(any(AnswerSheet.class));
        verify(answerDetailMapper).insert(any(AnswerDetail.class));
        verify(valueOperations).set(eq("exam:timeout:" + response.getAnswerSheetId()),
                eq("1"), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("startExam: should resume with existing answers on breakpoint continuation")
    void startExam_shouldResumeWithExistingAnswers() {
        AnswerSheet existingSheet = AnswerSheet.builder()
                .id(5L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").remainingSeconds(1800)
                .startTime(LocalDateTime.now().minusMinutes(30))
                .tabSwitchCount(0).build();

        AnswerDetail detail = AnswerDetail.builder()
                .id(10L).answerSheetId(5L).questionId(100L).examQuestionId(1L)
                .studentAnswer("A").build();

        Question q = Question.builder()
                .id(100L).content("What is Java?").questionType("SINGLE_CHOICE")
                .correctAnswer("A").score(20).status("ACTIVE").build();

        ExamQuestion eq = ExamQuestion.builder()
                .id(1L).examId(1L).questionId(100L).sortOrder(1).build();

        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerSheetMapper.selectCount(any())).thenReturn(0L);
        when(answerSheetMapper.selectOne(any())).thenReturn(existingSheet);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(questionMapper.selectById(100L)).thenReturn(q);
        when(examQuestionMapper.selectById(1L)).thenReturn(eq);
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(1500L);

        ExamStartResponse response = examService.startExam(1L, 1L);

        assertTrue(response.getResumed());
        assertEquals(5L, response.getAnswerSheetId());
        assertEquals(1, response.getAttemptNo());
        assertEquals(1500, response.getRemainingSeconds());
        assertEquals(1, response.getQuestions().size());
        assertEquals(1, response.getExistingAnswers().size());
        assertEquals("A", response.getExistingAnswers().get(0).getStudentAnswer());
        assertEquals(100L, response.getExistingAnswers().get(0).getQuestionId());

        verify(answerSheetMapper, never()).insert(any());
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
    @DisplayName("submitExam: should auto-grade SINGLE_CHOICE, MULTI_CHOICE, and TRUE_FALSE correctly")
    void submitExam_shouldAutoGradeObjectiveQuestions() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").tabSwitchCount(0).build();

        // Four questions: SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, SHORT_ANSWER
        AnswerDetail d1 = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(101L).examQuestionId(201L).build();
        AnswerDetail d2 = AnswerDetail.builder()
                .id(2L).answerSheetId(1L).questionId(102L).examQuestionId(202L).build();
        AnswerDetail d3 = AnswerDetail.builder()
                .id(3L).answerSheetId(1L).questionId(103L).examQuestionId(203L).build();
        AnswerDetail d4 = AnswerDetail.builder()
                .id(4L).answerSheetId(1L).questionId(104L).examQuestionId(204L).build();

        Question q1 = Question.builder()
                .id(101L).questionType("SINGLE_CHOICE").correctAnswer("A").score(20).status("ACTIVE").build();
        Question q2 = Question.builder()
                .id(102L).questionType("MULTI_CHOICE").correctAnswer("A,B").score(20).status("ACTIVE").build();
        Question q3 = Question.builder()
                .id(103L).questionType("TRUE_FALSE").correctAnswer("True").score(20).status("ACTIVE").build();
        Question q4 = Question.builder()
                .id(104L).questionType("SHORT_ANSWER").correctAnswer("N/A").score(40).status("ACTIVE").build();

        ExamQuestion eq1 = ExamQuestion.builder().id(201L).examId(1L).questionId(101L).sortOrder(1).build();
        ExamQuestion eq2 = ExamQuestion.builder().id(202L).examId(1L).questionId(102L).sortOrder(2).build();
        ExamQuestion eq3 = ExamQuestion.builder().id(203L).examId(1L).questionId(103L).sortOrder(3).build();
        ExamQuestion eq4 = ExamQuestion.builder().id(204L).examId(1L).questionId(104L).sortOrder(4).build();

        // Pre-set student answers on the detail objects.
        // In the actual flow, submitExam saves answers via selectOne+updateById before auto-grading.
        // Since our selectOne mock returns null (skipping the save loop), we pre-populate answers
        // to ensure autoGrade can find them during grading.
        d1.setStudentAnswer("A");       // SINGLE_CHOICE: correct
        d2.setStudentAnswer("A,B");     // MULTI_CHOICE: correct
        d3.setStudentAnswer("True");    // TRUE_FALSE: correct
        d4.setStudentAnswer("essay answer"); // SHORT_ANSWER: needs manual grading

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null); // null means not blocked (proceed)
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(Arrays.asList(d1, d2, d3, d4));
        when(questionMapper.selectById(101L)).thenReturn(q1);
        when(questionMapper.selectById(102L)).thenReturn(q2);
        when(questionMapper.selectById(103L)).thenReturn(q3);
        when(questionMapper.selectById(104L)).thenReturn(q4);
        when(examQuestionMapper.selectById(201L)).thenReturn(eq1);
        when(examQuestionMapper.selectById(202L)).thenReturn(eq2);
        when(examQuestionMapper.selectById(203L)).thenReturn(eq3);
        when(examQuestionMapper.selectById(204L)).thenReturn(eq4);

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        AnswerSubmitRequest.AnswerItem a1 = new AnswerSubmitRequest.AnswerItem();
        a1.setQuestionId(101L); a1.setExamQuestionId(201L); a1.setAnswer("A"); // correct
        AnswerSubmitRequest.AnswerItem a2 = new AnswerSubmitRequest.AnswerItem();
        a2.setQuestionId(102L); a2.setExamQuestionId(202L); a2.setAnswer("A,B"); // correct
        AnswerSubmitRequest.AnswerItem a3 = new AnswerSubmitRequest.AnswerItem();
        a3.setQuestionId(103L); a3.setExamQuestionId(203L); a3.setAnswer("True"); // correct
        AnswerSubmitRequest.AnswerItem a4 = new AnswerSubmitRequest.AnswerItem();
        a4.setQuestionId(104L); a4.setExamQuestionId(204L); a4.setAnswer("essay answer");
        req.setAnswers(Arrays.asList(a1, a2, a3, a4));

        AnswerSheet result = examService.submitExam(req, 1L);

        // 3 objective questions correct = 60 points; SHORT_ANSWER needs manual grading
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals(0, result.getRemainingSeconds());

        // All 4 answer details should be updated (3 graded + 1 marked for manual grading)
        verify(answerDetailMapper, times(4)).updateById(any(AnswerDetail.class));

        // Verify individual answer grading via ArgumentCaptor
        ArgumentCaptor<AnswerDetail> detailCaptor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper, times(4)).updateById(detailCaptor.capture());

        List<AnswerDetail> updatedDetails = detailCaptor.getAllValues();
        // d1: SINGLE_CHOICE correct -> isCorrect=1, scoreEarned=20
        assertEquals(1, updatedDetails.get(0).getIsCorrect());
        // d2: MULTI_CHOICE correct -> isCorrect=1, scoreEarned=20
        assertEquals(1, updatedDetails.get(1).getIsCorrect());
        // d3: TRUE_FALSE correct -> isCorrect=1, scoreEarned=20
        assertEquals(1, updatedDetails.get(2).getIsCorrect());
        // d4: SHORT_ANSWER -> no isCorrect set, has manual grading note
        assertEquals("待人工批改", updatedDetails.get(3).getGradingNote());

        // Not all graded (SHORT_ANSWER pending) -> no Grade record created
        verify(gradeMapper, never()).insert(any(Grade.class));

        // Redis timeout key should be deleted
        verify(redisTemplate).delete("exam:timeout:1");
    }

    @Test
    @DisplayName("submitExam: should throw BusinessException on duplicate submission (Redis idempotent check)")
    void submitExam_shouldThrowOnDuplicateSubmission() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        // Redis setIfAbsent returns false -> key already exists -> duplicate
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> examService.submitExam(req, 1L));
        assertTrue(ex.getMessage().contains("重复提交"));

        // Should not reach the grading logic
        verify(examMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("submitExam: should give full score when question has been deleted")
    void submitExam_shouldGiveFullScoreForDeletedQuestion() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS").tabSwitchCount(0).build();

        AnswerDetail detail = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L)
                .studentAnswer("wrong").build();

        // Question exists but has DELETED status
        Question deletedQ = Question.builder()
                .id(100L).questionType("SINGLE_CHOICE").correctAnswer("A")
                .score(10).status("DELETED").build();

        ExamQuestion eq = ExamQuestion.builder()
                .id(200L).examId(1L).questionId(100L).sortOrder(1).build();

        when(answerSheetMapper.selectById(1L)).thenReturn(sheet);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(questionMapper.selectById(100L)).thenReturn(deletedQ);
        when(examQuestionMapper.selectById(200L)).thenReturn(eq);

        AnswerSubmitRequest req = new AnswerSubmitRequest();
        req.setAnswerSheetId(1L);
        req.setAnswers(Collections.emptyList());

        AnswerSheet result = examService.submitExam(req, 1L);

        // Deleted question gets full score (10 points) even though student answered wrong
        assertEquals("SUBMITTED", result.getStatus());

        ArgumentCaptor<AnswerDetail> captor = ArgumentCaptor.forClass(AnswerDetail.class);
        verify(answerDetailMapper).updateById(captor.capture());
        AnswerDetail updated = captor.getValue();
        assertEquals(1, updated.getIsCorrect());
        assertEquals("题目已删除，自动给满分", updated.getGradingNote());
    }

    // ========================================================================
    // reportTabSwitch tests
    // ========================================================================

    @Test
    @DisplayName("reportTabSwitch: should auto-submit when tab switches exceed anti-cheat limit")
    void reportTabSwitch_shouldAutoSubmitWhenExceedsLimit() {
        // tabSwitchCount = 3, after increment becomes 4, maxTabSwitches = 3 -> exceeds
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS").tabSwitchCount(3).build();

        AnswerDetail detail = AnswerDetail.builder()
                .id(1L).answerSheetId(1L).questionId(100L).examQuestionId(200L).build();

        Question q = Question.builder()
                .id(100L).questionType("SINGLE_CHOICE").correctAnswer("A")
                .score(20).status("ACTIVE").build();

        ExamQuestion eq = ExamQuestion.builder()
                .id(200L).examId(1L).questionId(100L).sortOrder(1).build();

        when(answerSheetMapper.selectOne(any())).thenReturn(sheet);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);
        when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(questionMapper.selectById(100L)).thenReturn(q);
        when(examQuestionMapper.selectById(200L)).thenReturn(eq);

        examService.reportTabSwitch(1L, 1L);

        // First updateById: increment tabSwitchCount to 4
        // Second updateById (from autoGrade): set status to AUTO_SUBMITTED
        verify(answerSheetMapper, atLeast(2)).updateById(any(AnswerSheet.class));

        // Verify auto-submit was triggered with AUTO_SUBMITTED status
        verify(answerSheetMapper, atLeastOnce()).updateById(argThat(s ->
                "AUTO_SUBMITTED".equals(s.getStatus())));

        // Redis timeout key should be cleaned up by autoGrade
        verify(redisTemplate).delete("exam:timeout:1");
    }

    @Test
    @DisplayName("reportTabSwitch: should only increment count when under the limit")
    void reportTabSwitch_shouldOnlyIncrementWhenUnderLimit() {
        // tabSwitchCount = 1, after increment becomes 2, maxTabSwitches = 3 -> under limit
        AnswerSheet sheet = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS").tabSwitchCount(1).build();

        when(answerSheetMapper.selectOne(any())).thenReturn(sheet);
        when(examMapper.selectById(1L)).thenReturn(publishedExam);

        examService.reportTabSwitch(1L, 1L);

        // Only one updateById call (increment tabSwitchCount), no auto-submit
        verify(answerSheetMapper, times(1)).updateById(any(AnswerSheet.class));
        verify(answerSheetMapper, never()).updateById(argThat(s ->
                "AUTO_SUBMITTED".equals(s.getStatus())));
        verify(answerDetailMapper, never()).selectList(any());
    }
}
