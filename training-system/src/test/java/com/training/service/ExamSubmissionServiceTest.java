package com.training.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.dto.request.AnswerSubmitRequest;
import com.training.dto.request.SaveAnswerRequest;
import com.training.dto.response.ExamStartResponse;
import com.training.entity.Exam;
import com.training.entity.ExamQuestion;
import com.training.entity.ExamSubmission;
import com.training.entity.SubmissionAnswer;
import com.training.enums.ExamStatus;
import com.training.enums.SubmissionStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ExamMapper;
import com.training.mapper.ExamQuestionMapper;
import com.training.mapper.ExamSubmissionMapper;
import com.training.mapper.SubmissionAnswerMapper;
import com.training.service.impl.ExamSubmissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamSubmissionServiceTest {

    @Mock
    private ExamMapper examMapper;

    @Mock
    private ExamSubmissionMapper examSubmissionMapper;

    @Mock
    private SubmissionAnswerMapper submissionAnswerMapper;

    @Mock
    private ExamQuestionMapper examQuestionMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private ExamSubmissionServiceImpl examSubmissionService;

    // ==================== startExam tests ====================

    @Test
    void testStartExam_Success() throws Exception {
        Long examId = 1L;
        Long studentId = 1L;

        // Mock exam
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setStatus(ExamStatus.PUBLISHED.name());
        exam.setMaxAttempts(3);
        exam.setDurationMinutes(60);
        when(examMapper.selectById(examId)).thenReturn(exam);

        // No in-progress submission
        when(examSubmissionMapper.selectInProgressByExamAndStudent(examId, studentId)).thenReturn(null);

        // Zero previous attempts
        when(examSubmissionMapper.countByExamAndStudent(examId, studentId)).thenReturn(0);

        // Mock insert to set ID
        doAnswer(invocation -> {
            ExamSubmission sub = invocation.getArgument(0);
            sub.setId(1L);
            return 1;
        }).when(examSubmissionMapper).insert(any(ExamSubmission.class));

        // Mock exam questions with JSON snapshots
        String snapshot1 = "{\"content\":\"What is Java?\",\"questionType\":\"SINGLE_CHOICE\",\"options\":\"A,B,C,D\",\"correctAnswer\":\"A\"}";
        String snapshot2 = "{\"content\":\"Is Java OOP?\",\"questionType\":\"TRUE_FALSE\",\"correctAnswer\":\"true\"}";

        ExamQuestion eq1 = new ExamQuestion();
        eq1.setId(1L);
        eq1.setExamId(examId);
        eq1.setQuestionId(1L);
        eq1.setQuestionSnapshot(snapshot1);
        eq1.setScore(new BigDecimal("50"));

        ExamQuestion eq2 = new ExamQuestion();
        eq2.setId(2L);
        eq2.setExamId(examId);
        eq2.setQuestionId(2L);
        eq2.setQuestionSnapshot(snapshot2);
        eq2.setScore(new BigDecimal("50"));

        when(examQuestionMapper.selectByExamId(examId)).thenReturn(Arrays.asList(eq1, eq2));

        // Mock batch insert answers
        when(submissionAnswerMapper.batchInsert(anyList())).thenReturn(2);

        // Mock Redis opsForValue for start time
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock objectMapper for buildQuestionItems
        Map<String, Object> snapMap1 = new HashMap<>();
        snapMap1.put("content", "What is Java?");
        snapMap1.put("questionType", "SINGLE_CHOICE");
        snapMap1.put("options", "A,B,C,D");
        snapMap1.put("correctAnswer", "A");

        Map<String, Object> snapMap2 = new HashMap<>();
        snapMap2.put("content", "Is Java OOP?");
        snapMap2.put("questionType", "TRUE_FALSE");
        snapMap2.put("correctAnswer", "true");

        when(objectMapper.readValue(eq(snapshot1), any(TypeReference.class))).thenReturn(snapMap1);
        when(objectMapper.readValue(eq(snapshot2), any(TypeReference.class))).thenReturn(snapMap2);

        // Execute
        ExamStartResponse response = examSubmissionService.startExam(examId, studentId);

        // Verify
        assertNotNull(response);
        assertEquals(1L, response.getSubmissionId());
        assertEquals(examId, response.getExamId());
        assertEquals(60, response.getDurationMinutes());
        assertEquals(3600L, response.getRemainingSeconds());
        assertEquals(2, response.getQuestions().size());

        // Verify submission was created
        verify(examSubmissionMapper).insert(any(ExamSubmission.class));

        // Verify Redis start time was stored
        verify(valueOperations).set(eq("exam:start:1"), anyString(), eq(65L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testStartExam_ResumeInProgress() throws Exception {
        Long examId = 1L;
        Long studentId = 1L;

        // Mock exam
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setStatus(ExamStatus.PUBLISHED.name());
        exam.setDurationMinutes(60);
        when(examMapper.selectById(examId)).thenReturn(exam);

        // Existing in-progress submission (started 10 minutes ago)
        ExamSubmission existingSubmission = new ExamSubmission();
        existingSubmission.setId(1L);
        existingSubmission.setExamId(examId);
        existingSubmission.setStudentId(studentId);
        existingSubmission.setStartTime(LocalDateTime.now().minusMinutes(10));
        existingSubmission.setEndTime(LocalDateTime.now().plusMinutes(50));
        existingSubmission.setStatus(SubmissionStatus.IN_PROGRESS.name());
        when(examSubmissionMapper.selectInProgressByExamAndStudent(examId, studentId)).thenReturn(existingSubmission);

        // Mock exam questions
        String snapshot1 = "{\"content\":\"What is Java?\",\"questionType\":\"SINGLE_CHOICE\",\"options\":\"A,B,C,D\"}";
        ExamQuestion eq1 = new ExamQuestion();
        eq1.setId(1L);
        eq1.setExamId(examId);
        eq1.setQuestionId(1L);
        eq1.setQuestionSnapshot(snapshot1);
        eq1.setScore(new BigDecimal("100"));
        when(examQuestionMapper.selectByExamId(examId)).thenReturn(Collections.singletonList(eq1));

        // Mock Redis opsForHash returning saved answers
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Map<Object, Object> redisAnswers = new HashMap<>();
        redisAnswers.put("1", "A");
        when(hashOperations.entries("exam:progress:1")).thenReturn(redisAnswers);

        // Mock objectMapper for buildQuestionItems
        Map<String, Object> snapMap = new HashMap<>();
        snapMap.put("content", "What is Java?");
        snapMap.put("questionType", "SINGLE_CHOICE");
        snapMap.put("options", "A,B,C,D");
        when(objectMapper.readValue(eq(snapshot1), any(TypeReference.class))).thenReturn(snapMap);

        // Execute
        ExamStartResponse response = examSubmissionService.startExam(examId, studentId);

        // Verify
        assertNotNull(response);
        assertTrue(response.getRemainingSeconds() > 0);
        assertEquals(1, response.getQuestions().size());
        assertEquals("A", response.getQuestions().get(0).getStudentAnswer());

        // Verify no new submission was created
        verify(examSubmissionMapper, never()).insert(any(ExamSubmission.class));
    }

    @Test
    void testStartExam_MaxAttemptsReached() {
        Long examId = 1L;
        Long studentId = 1L;

        Exam exam = new Exam();
        exam.setId(examId);
        exam.setStatus(ExamStatus.PUBLISHED.name());
        exam.setMaxAttempts(3);
        when(examMapper.selectById(examId)).thenReturn(exam);

        when(examSubmissionMapper.selectInProgressByExamAndStudent(examId, studentId)).thenReturn(null);
        when(examSubmissionMapper.countByExamAndStudent(examId, studentId)).thenReturn(3);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> examSubmissionService.startExam(examId, studentId));

        assertTrue(exception.getMessage().contains("最大补考次数"));
    }

    // ==================== submitExam tests ====================

    @Test
    void testSubmitExam_Success_AutoGrade() throws Exception {
        Long submissionId = 1L;
        Long studentId = 1L;

        // Build request with 4 answers
        AnswerSubmitRequest request = new AnswerSubmitRequest();
        request.setSubmissionId(submissionId);

        List<AnswerSubmitRequest.AnswerItem> answerItems = new ArrayList<>();
        AnswerSubmitRequest.AnswerItem item1 = new AnswerSubmitRequest.AnswerItem();
        item1.setQuestionId(1L);
        item1.setAnswer("A");
        answerItems.add(item1);

        AnswerSubmitRequest.AnswerItem item2 = new AnswerSubmitRequest.AnswerItem();
        item2.setQuestionId(2L);
        item2.setAnswer("A,B,C");
        answerItems.add(item2);

        AnswerSubmitRequest.AnswerItem item3 = new AnswerSubmitRequest.AnswerItem();
        item3.setQuestionId(3L);
        item3.setAnswer("false");
        answerItems.add(item3);

        AnswerSubmitRequest.AnswerItem item4 = new AnswerSubmitRequest.AnswerItem();
        item4.setQuestionId(4L);
        item4.setAnswer("答案");
        answerItems.add(item4);

        request.setAnswers(answerItems);

        // Mock Redis lock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("exam:submit:1", "1", 30, TimeUnit.SECONDS)).thenReturn(true);

        // Mock submission (IN_PROGRESS, endTime in future)
        ExamSubmission submission = new ExamSubmission();
        submission.setId(submissionId);
        submission.setExamId(1L);
        submission.setStudentId(studentId);
        submission.setStatus(SubmissionStatus.IN_PROGRESS.name());
        submission.setEndTime(LocalDateTime.now().plusMinutes(30));
        when(examSubmissionMapper.selectById(submissionId)).thenReturn(submission);

        // Define question snapshots
        String snapshot1 = "{\"questionType\":\"SINGLE_CHOICE\",\"correctAnswer\":\"A\",\"score\":25}";
        String snapshot2 = "{\"questionType\":\"MULTI_CHOICE\",\"correctAnswer\":\"A,B,C\",\"score\":25}";
        String snapshot3 = "{\"questionType\":\"TRUE_FALSE\",\"correctAnswer\":\"true\",\"score\":25}";
        String snapshot4 = "{\"questionType\":\"FILL_BLANK\",\"correctAnswer\":\"答案\",\"score\":25}";

        // Create SubmissionAnswer objects
        SubmissionAnswer sa1 = new SubmissionAnswer();
        sa1.setId(101L);
        sa1.setSubmissionId(submissionId);
        sa1.setQuestionId(1L);
        sa1.setQuestionSnapshot(snapshot1);
        sa1.setAwardedScore(BigDecimal.ZERO);

        SubmissionAnswer sa2 = new SubmissionAnswer();
        sa2.setId(102L);
        sa2.setSubmissionId(submissionId);
        sa2.setQuestionId(2L);
        sa2.setQuestionSnapshot(snapshot2);
        sa2.setAwardedScore(BigDecimal.ZERO);

        SubmissionAnswer sa3 = new SubmissionAnswer();
        sa3.setId(103L);
        sa3.setSubmissionId(submissionId);
        sa3.setQuestionId(3L);
        sa3.setQuestionSnapshot(snapshot3);
        sa3.setAwardedScore(BigDecimal.ZERO);

        SubmissionAnswer sa4 = new SubmissionAnswer();
        sa4.setId(104L);
        sa4.setSubmissionId(submissionId);
        sa4.setQuestionId(4L);
        sa4.setQuestionSnapshot(snapshot4);
        sa4.setAwardedScore(BigDecimal.ZERO);

        // Mock selectBySubmissionAndQuestion for answer update loop
        when(submissionAnswerMapper.selectBySubmissionAndQuestion(submissionId, 1L)).thenReturn(sa1);
        when(submissionAnswerMapper.selectBySubmissionAndQuestion(submissionId, 2L)).thenReturn(sa2);
        when(submissionAnswerMapper.selectBySubmissionAndQuestion(submissionId, 3L)).thenReturn(sa3);
        when(submissionAnswerMapper.selectBySubmissionAndQuestion(submissionId, 4L)).thenReturn(sa4);

        // Mock selectBySubmissionId for auto-grading (returns answers with studentAnswer set)
        // The loop above sets studentAnswer on the sa objects, so they will have the correct values
        when(submissionAnswerMapper.selectBySubmissionId(submissionId))
                .thenReturn(Arrays.asList(sa1, sa2, sa3, sa4));

        // Mock objectMapper for autoGradeAnswers
        Map<String, Object> map1 = new HashMap<>();
        map1.put("questionType", "SINGLE_CHOICE");
        map1.put("correctAnswer", "A");
        map1.put("score", 25);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("questionType", "MULTI_CHOICE");
        map2.put("correctAnswer", "A,B,C");
        map2.put("score", 25);

        Map<String, Object> map3 = new HashMap<>();
        map3.put("questionType", "TRUE_FALSE");
        map3.put("correctAnswer", "true");
        map3.put("score", 25);

        Map<String, Object> map4 = new HashMap<>();
        map4.put("questionType", "FILL_BLANK");
        map4.put("correctAnswer", "答案");
        map4.put("score", 25);

        when(objectMapper.readValue(eq(snapshot1), any(TypeReference.class))).thenReturn(map1);
        when(objectMapper.readValue(eq(snapshot2), any(TypeReference.class))).thenReturn(map2);
        when(objectMapper.readValue(eq(snapshot3), any(TypeReference.class))).thenReturn(map3);
        when(objectMapper.readValue(eq(snapshot4), any(TypeReference.class))).thenReturn(map4);

        // Mock Redis delete for cleanup
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Mock exam for pass score check
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setPassScore(new BigDecimal("60"));
        when(examMapper.selectById(1L)).thenReturn(exam);

        // Execute
        Map<String, Object> result = examSubmissionService.submitExam(request, studentId);

        // Verify result
        assertNotNull(result);
        assertEquals(submissionId, result.get("submissionId"));

        // Total score: q1 correct(25) + q2 correct(25) + q3 wrong(0) + q4 correct(25) = 75
        BigDecimal expectedTotal = new BigDecimal("75");
        assertEquals(0, expectedTotal.compareTo((BigDecimal) result.get("totalScore")));
        assertEquals(true, result.get("passed"));

        // Verify scoring updates
        verify(submissionAnswerMapper).updateScore(101L, 1, new BigDecimal("25")); // q1 correct
        verify(submissionAnswerMapper).updateScore(102L, 1, new BigDecimal("25")); // q2 correct
        verify(submissionAnswerMapper).updateScore(103L, 0, BigDecimal.ZERO);      // q3 wrong
        verify(submissionAnswerMapper).updateScore(104L, 1, new BigDecimal("25")); // q4 correct

        // Verify submission status updated to SUBMITTED
        verify(examSubmissionMapper).updateStatus(eq(submissionId), eq(SubmissionStatus.SUBMITTED.name()),
                any(LocalDateTime.class), eq(expectedTotal));
    }

    @Test
    void testSubmitExam_DuplicateSubmit() {
        Long submissionId = 1L;
        Long studentId = 1L;

        AnswerSubmitRequest request = new AnswerSubmitRequest();
        request.setSubmissionId(submissionId);

        // Mock Redis setIfAbsent returns false (duplicate)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("exam:submit:1", "1", 30, TimeUnit.SECONDS)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> examSubmissionService.submitExam(request, studentId));

        assertTrue(exception.getMessage().contains("重复提交"));
    }

    @Test
    void testSubmitExam_TimedOut() throws Exception {
        Long submissionId = 1L;
        Long studentId = 1L;

        AnswerSubmitRequest request = new AnswerSubmitRequest();
        request.setSubmissionId(submissionId);
        request.setAnswers(Collections.emptyList());

        // Mock Redis lock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("exam:submit:1", "1", 30, TimeUnit.SECONDS)).thenReturn(true);

        // Mock submission with endTime in the past
        ExamSubmission submission = new ExamSubmission();
        submission.setId(submissionId);
        submission.setExamId(1L);
        submission.setStudentId(studentId);
        submission.setStatus(SubmissionStatus.IN_PROGRESS.name());
        submission.setEndTime(LocalDateTime.now().minusMinutes(5));
        when(examSubmissionMapper.selectById(submissionId)).thenReturn(submission);

        // Mock selectBySubmissionId for auto-grading (empty answers)
        when(submissionAnswerMapper.selectBySubmissionId(submissionId)).thenReturn(Collections.emptyList());

        // Mock Redis delete
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Mock exam for pass score
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setPassScore(new BigDecimal("60"));
        when(examMapper.selectById(1L)).thenReturn(exam);

        // Execute
        Map<String, Object> result = examSubmissionService.submitExam(request, studentId);

        // Verify status is set to TIMED_OUT
        verify(examSubmissionMapper).updateStatus(eq(submissionId), eq(SubmissionStatus.TIMED_OUT.name()),
                any(LocalDateTime.class), any(BigDecimal.class));
    }

    // ==================== saveAnswer tests ====================

    @Test
    void testSaveAnswer_Success() {
        Long submissionId = 1L;
        Long questionId = 1L;
        Long studentId = 1L;

        SaveAnswerRequest request = new SaveAnswerRequest();
        request.setSubmissionId(submissionId);
        request.setQuestionId(questionId);
        request.setAnswer("A");

        // Mock submission (IN_PROGRESS, belongs to student, endTime in future)
        ExamSubmission submission = new ExamSubmission();
        submission.setId(submissionId);
        submission.setStudentId(studentId);
        submission.setStatus(SubmissionStatus.IN_PROGRESS.name());
        submission.setEndTime(LocalDateTime.now().plusMinutes(30));
        when(examSubmissionMapper.selectById(submissionId)).thenReturn(submission);

        // Mock Redis opsForHash
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // Mock existing answer in DB
        SubmissionAnswer existingAnswer = new SubmissionAnswer();
        existingAnswer.setId(10L);
        existingAnswer.setSubmissionId(submissionId);
        existingAnswer.setQuestionId(questionId);
        when(submissionAnswerMapper.selectBySubmissionAndQuestion(submissionId, questionId))
                .thenReturn(existingAnswer);

        // Execute
        examSubmissionService.saveAnswer(request, studentId);

        // Verify Redis put is called
        verify(hashOperations).put("exam:progress:1", "1", "A");

        // Verify DB update is called
        verify(submissionAnswerMapper).updateAnswer(eq(10L), eq("A"), any(LocalDateTime.class));
    }

    // ==================== autoSubmitTimedOut tests ====================

    @Test
    void testAutoSubmitTimedOut() {
        // Submission 1: timed out (endTime in past)
        ExamSubmission sub1 = new ExamSubmission();
        sub1.setId(1L);
        sub1.setExamId(1L);
        sub1.setStudentId(1L);
        sub1.setStatus(SubmissionStatus.IN_PROGRESS.name());
        sub1.setEndTime(LocalDateTime.now().minusMinutes(10));

        // Submission 2: still valid (endTime in future)
        ExamSubmission sub2 = new ExamSubmission();
        sub2.setId(2L);
        sub2.setExamId(2L);
        sub2.setStudentId(2L);
        sub2.setStatus(SubmissionStatus.IN_PROGRESS.name());
        sub2.setEndTime(LocalDateTime.now().plusMinutes(30));

        when(examSubmissionMapper.selectByStatus(SubmissionStatus.IN_PROGRESS.name()))
                .thenReturn(Arrays.asList(sub1, sub2));

        // Mock Redis for doAutoSubmit of sub1
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("exam:progress:1")).thenReturn(Collections.emptyMap());

        // Mock answers for timed out submission (empty list for simplicity)
        when(submissionAnswerMapper.selectBySubmissionId(1L)).thenReturn(Collections.emptyList());

        // Mock Redis delete
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Execute
        examSubmissionService.autoSubmitTimedOut();

        // Verify only the timed out submission (sub1) gets auto-submitted
        verify(examSubmissionMapper).updateStatus(eq(1L), eq(SubmissionStatus.AUTO_SUBMITTED.name()),
                any(LocalDateTime.class), eq(BigDecimal.ZERO));

        // Verify sub2 was NOT auto-submitted
        verify(examSubmissionMapper, never()).updateStatus(eq(2L), anyString(),
                any(LocalDateTime.class), any(BigDecimal.class));
    }
}
