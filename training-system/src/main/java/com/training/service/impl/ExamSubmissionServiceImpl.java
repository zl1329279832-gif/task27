package com.training.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.dto.request.AnswerSubmitRequest;
import com.training.dto.request.HeartbeatRequest;
import com.training.dto.request.SaveAnswerRequest;
import com.training.dto.response.ExamStartResponse;
import com.training.entity.Exam;
import com.training.entity.ExamQuestion;
import com.training.entity.ExamSubmission;
import com.training.entity.SubmissionAnswer;
import com.training.enums.ExamStatus;
import com.training.enums.QuestionType;
import com.training.enums.SubmissionStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ExamMapper;
import com.training.mapper.ExamQuestionMapper;
import com.training.mapper.ExamSubmissionMapper;
import com.training.mapper.SubmissionAnswerMapper;
import com.training.service.ExamSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExamSubmissionServiceImpl implements ExamSubmissionService {

    private final ExamMapper examMapper;
    private final ExamSubmissionMapper examSubmissionMapper;
    private final SubmissionAnswerMapper submissionAnswerMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PROGRESS_KEY = "exam:progress:";
    private static final String REDIS_SUBMIT_KEY = "exam:submit:";
    private static final String REDIS_START_KEY = "exam:start:";

    @Override
    public ExamStartResponse startExam(Long examId, Long studentId) {
        log.info("开始考试, examId: {}, studentId: {}", examId, studentId);

        // 1. Get exam and check status
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!ExamStatus.PUBLISHED.name().equals(exam.getStatus())) {
            throw new BusinessException("考试未发布，无法参加");
        }

        // 2. Check existing IN_PROGRESS submission (resume scenario)
        ExamSubmission existingSubmission = examSubmissionMapper.selectInProgressByExamAndStudent(examId, studentId);
        if (existingSubmission != null) {
            return resumeExam(existingSubmission, exam);
        }

        // 3. Check attempt count
        int attemptCount = examSubmissionMapper.countByExamAndStudent(examId, studentId);
        if (attemptCount >= exam.getMaxAttempts()) {
            throw new BusinessException("已达到最大补考次数");
        }

        // 4. Create new submission
        LocalDateTime now = LocalDateTime.now();
        ExamSubmission submission = ExamSubmission.builder()
                .examId(examId)
                .studentId(studentId)
                .startTime(now)
                .endTime(now.plusMinutes(exam.getDurationMinutes()))
                .status(SubmissionStatus.IN_PROGRESS.name())
                .totalScore(BigDecimal.ZERO)
                .tabSwitchCount(0)
                .attemptNumber(attemptCount + 1)
                .build();
        examSubmissionMapper.insert(submission);

        // 5. Get exam questions
        List<ExamQuestion> examQuestions = examQuestionMapper.selectByExamId(examId);

        // 6. Create submission answers for each exam question
        List<SubmissionAnswer> submissionAnswers = new ArrayList<>();
        for (ExamQuestion examQuestion : examQuestions) {
            SubmissionAnswer answer = SubmissionAnswer.builder()
                    .submissionId(submission.getId())
                    .questionId(examQuestion.getQuestionId())
                    .questionSnapshot(examQuestion.getQuestionSnapshot())
                    .studentAnswer(null)
                    .isCorrect(null)
                    .awardedScore(BigDecimal.ZERO)
                    .build();
            submissionAnswers.add(answer);
        }
        if (!submissionAnswers.isEmpty()) {
            submissionAnswerMapper.batchInsert(submissionAnswers);
        }

        // 7. Store start time in Redis with TTL = durationMinutes + 5 minutes
        String startKey = REDIS_START_KEY + submission.getId();
        redisTemplate.opsForValue().set(startKey,
                String.valueOf(System.currentTimeMillis()),
                exam.getDurationMinutes() + 5, TimeUnit.MINUTES);

        // 8. Build response (no correctAnswer exposed)
        long remainingSeconds = (long) exam.getDurationMinutes() * 60;
        List<ExamStartResponse.QuestionItem> questionItems = buildQuestionItems(examQuestions, null);

        log.info("考试开始成功, submissionId: {}, examId: {}, studentId: {}", submission.getId(), examId, studentId);
        return ExamStartResponse.builder()
                .submissionId(submission.getId())
                .examId(examId)
                .durationMinutes(exam.getDurationMinutes())
                .remainingSeconds(remainingSeconds)
                .questions(questionItems)
                .build();
    }

    /**
     * Resume an existing in-progress exam submission.
     */
    private ExamStartResponse resumeExam(ExamSubmission submission, Exam exam) {
        log.info("恢复考试, submissionId: {}", submission.getId());

        // Calculate remaining time
        long remainingSeconds = Duration.between(LocalDateTime.now(), submission.getEndTime()).getSeconds();
        if (remainingSeconds <= 0) {
            // Auto-submit timed out exam
            log.info("考试已超时, submissionId: {}, 自动提交", submission.getId());
            doAutoSubmit(submission);
            throw new BusinessException("考试已超时");
        }

        // Get exam questions
        List<ExamQuestion> examQuestions = examQuestionMapper.selectByExamId(submission.getExamId());

        // Load saved answers: try Redis first, fall back to DB
        Map<String, String> savedAnswers = loadSavedAnswers(submission.getId());

        // Build question items with saved answers
        List<ExamStartResponse.QuestionItem> questionItems = buildQuestionItems(examQuestions, savedAnswers);

        return ExamStartResponse.builder()
                .submissionId(submission.getId())
                .examId(submission.getExamId())
                .durationMinutes(exam.getDurationMinutes())
                .remainingSeconds(remainingSeconds)
                .questions(questionItems)
                .build();
    }

    /**
     * Load saved answers from Redis hash, falling back to DB if Redis is empty.
     */
    private Map<String, String> loadSavedAnswers(Long submissionId) {
        String progressKey = REDIS_PROGRESS_KEY + submissionId;
        Map<Object, Object> redisAnswers = redisTemplate.opsForHash().entries(progressKey);

        Map<String, String> result = new HashMap<>();
        if (redisAnswers != null && !redisAnswers.isEmpty()) {
            for (Map.Entry<Object, Object> entry : redisAnswers.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
        } else {
            // Fall back to DB
            List<SubmissionAnswer> dbAnswers = submissionAnswerMapper.selectBySubmissionId(submissionId);
            for (SubmissionAnswer answer : dbAnswers) {
                if (answer.getStudentAnswer() != null) {
                    result.put(answer.getQuestionId().toString(), answer.getStudentAnswer());
                }
            }
        }
        return result;
    }

    /**
     * Build question items for the response, excluding correctAnswer.
     */
    private List<ExamStartResponse.QuestionItem> buildQuestionItems(
            List<ExamQuestion> examQuestions, Map<String, String> savedAnswers) {
        List<ExamStartResponse.QuestionItem> items = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            try {
                Map<String, Object> snapshot = objectMapper.readValue(
                        eq.getQuestionSnapshot(), new TypeReference<Map<String, Object>>() {});

                ExamStartResponse.QuestionItem item = new ExamStartResponse.QuestionItem();
                item.setQuestionId(eq.getQuestionId());
                item.setContent((String) snapshot.get("content"));
                item.setQuestionType((String) snapshot.get("questionType"));
                item.setOptions((String) snapshot.get("options"));
                item.setScore(eq.getScore());

                // Set saved answer if available
                if (savedAnswers != null && savedAnswers.containsKey(eq.getQuestionId().toString())) {
                    item.setStudentAnswer(savedAnswers.get(eq.getQuestionId().toString()));
                }

                items.add(item);
            } catch (Exception e) {
                log.error("解析题目快照失败, examQuestionId: {}", eq.getId(), e);
                throw new BusinessException("题目数据异常");
            }
        }
        return items;
    }

    @Override
    public void saveAnswer(SaveAnswerRequest request, Long studentId) {
        log.info("保存答案, submissionId: {}, questionId: {}, studentId: {}",
                request.getSubmissionId(), request.getQuestionId(), studentId);

        // 1. Get submission and verify ownership and status
        ExamSubmission submission = examSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        if (!submission.getStudentId().equals(studentId)) {
            throw new BusinessException("无权操作此提交记录");
        }
        if (!SubmissionStatus.IN_PROGRESS.name().equals(submission.getStatus())) {
            throw new BusinessException("考试已结束，无法保存答案");
        }

        // 2. Check not timed out
        if (LocalDateTime.now().isAfter(submission.getEndTime())) {
            throw new BusinessException("考试已超时，无法保存答案");
        }

        // 3. Save to Redis hash
        String progressKey = REDIS_PROGRESS_KEY + request.getSubmissionId();
        redisTemplate.opsForHash().put(progressKey,
                request.getQuestionId().toString(),
                request.getAnswer() != null ? request.getAnswer() : "");

        // 4. Also update DB
        SubmissionAnswer answer = submissionAnswerMapper.selectBySubmissionAndQuestion(
                request.getSubmissionId(), request.getQuestionId());
        if (answer != null) {
            submissionAnswerMapper.updateAnswer(answer.getId(), request.getAnswer(), LocalDateTime.now());
        }
    }

    @Override
    public Map<String, Object> submitExam(AnswerSubmitRequest request, Long studentId) {
        log.info("提交考试, submissionId: {}, studentId: {}", request.getSubmissionId(), studentId);

        // 1. Idempotent check using Redis SETNX
        String submitKey = REDIS_SUBMIT_KEY + request.getSubmissionId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(submitKey, "1", 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("请勿重复提交");
        }

        try {
            // 2. Get submission and verify ownership and status
            ExamSubmission submission = examSubmissionMapper.selectById(request.getSubmissionId());
            if (submission == null) {
                throw new BusinessException("提交记录不存在");
            }
            if (!submission.getStudentId().equals(studentId)) {
                throw new BusinessException("无权操作此提交记录");
            }
            if (!SubmissionStatus.IN_PROGRESS.name().equals(submission.getStatus())) {
                throw new BusinessException("考试已结束，无法重复提交");
            }

            // 3. Determine final status based on timeout
            String finalStatus;
            if (LocalDateTime.now().isAfter(submission.getEndTime())) {
                finalStatus = SubmissionStatus.TIMED_OUT.name();
                log.info("考试已超时, submissionId: {}", submission.getId());
            } else {
                finalStatus = SubmissionStatus.SUBMITTED.name();
            }

            // 4. Apply answers from request
            if (request.getAnswers() != null) {
                for (AnswerSubmitRequest.AnswerItem answerItem : request.getAnswers()) {
                    SubmissionAnswer sa = submissionAnswerMapper.selectBySubmissionAndQuestion(
                            request.getSubmissionId(), answerItem.getQuestionId());
                    if (sa != null) {
                        submissionAnswerMapper.updateAnswer(sa.getId(), answerItem.getAnswer(), LocalDateTime.now());
                        sa.setStudentAnswer(answerItem.getAnswer());
                    }
                }
            }

            // 5. Auto-grade all answers
            List<SubmissionAnswer> allAnswers = submissionAnswerMapper.selectBySubmissionId(request.getSubmissionId());
            BigDecimal totalScore = autoGradeAnswers(allAnswers);

            // 6. Update submission status and score
            examSubmissionMapper.updateStatus(submission.getId(), finalStatus, LocalDateTime.now(), totalScore);

            // 7. Clean up Redis
            redisTemplate.delete(REDIS_PROGRESS_KEY + request.getSubmissionId());
            redisTemplate.delete(REDIS_START_KEY + request.getSubmissionId());

            // 8. Get exam pass score
            Exam exam = examMapper.selectById(submission.getExamId());

            Map<String, Object> result = new HashMap<>();
            result.put("submissionId", submission.getId());
            result.put("totalScore", totalScore);
            result.put("passed", exam != null && totalScore.compareTo(exam.getPassScore()) >= 0);

            log.info("考试提交成功, submissionId: {}, totalScore: {}", submission.getId(), totalScore);
            return result;
        } catch (BusinessException e) {
            // Remove lock on business error to allow retry
            redisTemplate.delete(submitKey);
            throw e;
        }
    }

    /**
     * Auto-grade all submission answers and return total score.
     */
    private BigDecimal autoGradeAnswers(List<SubmissionAnswer> answers) {
        BigDecimal totalScore = BigDecimal.ZERO;

        for (SubmissionAnswer answer : answers) {
            try {
                Map<String, Object> snapshot = objectMapper.readValue(
                        answer.getQuestionSnapshot(), new TypeReference<Map<String, Object>>() {});

                String correctAnswer = (String) snapshot.get("correctAnswer");
                String questionType = (String) snapshot.get("questionType");
                Object scoreObj = snapshot.get("score");
                BigDecimal questionScore = scoreObj instanceof Number
                        ? new BigDecimal(scoreObj.toString())
                        : answer.getAwardedScore();

                String studentAnswer = answer.getStudentAnswer();

                boolean isCorrect = gradeAnswer(studentAnswer, correctAnswer, questionType);

                if (isCorrect) {
                    submissionAnswerMapper.updateScore(answer.getId(), 1, questionScore);
                    totalScore = totalScore.add(questionScore);
                } else {
                    submissionAnswerMapper.updateScore(answer.getId(), 0, BigDecimal.ZERO);
                }
            } catch (Exception e) {
                log.error("自动阅卷失败, answerId: {}", answer.getId(), e);
                submissionAnswerMapper.updateScore(answer.getId(), 0, BigDecimal.ZERO);
            }
        }

        return totalScore;
    }

    /**
     * Grade a single answer based on question type.
     */
    private boolean gradeAnswer(String studentAnswer, String correctAnswer, String questionType) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return false;
        }
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return false;
        }

        String trimmedStudent = studentAnswer.trim();
        String trimmedCorrect = correctAnswer.trim();

        if (QuestionType.SINGLE_CHOICE.name().equals(questionType)) {
            return trimmedStudent.equalsIgnoreCase(trimmedCorrect);
        } else if (QuestionType.MULTI_CHOICE.name().equals(questionType)) {
            // Split by comma, sort, and compare sets
            List<String> studentChoices = Arrays.stream(trimmedStudent.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.toList());
            List<String> correctChoices = Arrays.stream(trimmedCorrect.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.toList());
            return studentChoices.equals(correctChoices);
        } else if (QuestionType.TRUE_FALSE.name().equals(questionType)) {
            return trimmedStudent.equalsIgnoreCase(trimmedCorrect);
        } else if (QuestionType.FILL_BLANK.name().equals(questionType)) {
            return trimmedStudent.equalsIgnoreCase(trimmedCorrect);
        }

        return false;
    }

    @Override
    public void handleHeartbeat(HeartbeatRequest request, Long studentId) {
        log.debug("处理心跳, submissionId: {}, studentId: {}", request.getSubmissionId(), studentId);

        // 1. Get submission and verify
        ExamSubmission submission = examSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        if (!submission.getStudentId().equals(studentId)) {
            throw new BusinessException("无权操作此提交记录");
        }
        if (!SubmissionStatus.IN_PROGRESS.name().equals(submission.getStatus())) {
            return;
        }

        // 2. Update tab switch count if provided
        if (request.getTabSwitchCount() != null) {
            submission.setTabSwitchCount(request.getTabSwitchCount());
            examSubmissionMapper.updateById(submission);

            // 3. If tab switch count exceeds threshold, auto-submit
            if (request.getTabSwitchCount() > 5) {
                log.warn("切屏次数超限, submissionId: {}, tabSwitchCount: {}, 自动提交",
                        submission.getId(), request.getTabSwitchCount());
                doAutoSubmit(submission);
            }
        }
    }

    @Override
    public ExamSubmission getSubmission(Long submissionId, Long studentId) {
        log.info("查询提交记录, submissionId: {}, studentId: {}", submissionId, studentId);
        ExamSubmission submission = examSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        if (studentId != null && !submission.getStudentId().equals(studentId)) {
            throw new BusinessException("无权查看此提交记录");
        }
        return submission;
    }

    @Override
    public List<SubmissionAnswer> getSubmissionAnswers(Long submissionId) {
        log.info("查询提交答案, submissionId: {}", submissionId);
        return submissionAnswerMapper.selectBySubmissionId(submissionId);
    }

    @Override
    public void autoSubmitTimedOut() {
        log.info("开始自动提交超时考试");

        // 1. Query all IN_PROGRESS submissions
        List<ExamSubmission> inProgressList = examSubmissionMapper.selectByStatus(SubmissionStatus.IN_PROGRESS.name());

        int count = 0;
        for (ExamSubmission submission : inProgressList) {
            // 2. Check if timed out
            if (LocalDateTime.now().isAfter(submission.getEndTime())) {
                try {
                    doAutoSubmit(submission);
                    count++;
                } catch (Exception e) {
                    log.error("自动提交失败, submissionId: {}", submission.getId(), e);
                }
            }
        }

        if (count > 0) {
            log.info("自动提交超时考试完成, 共处理{}条", count);
        }
    }

    /**
     * Perform auto-submit for a single submission: load saved answers from Redis,
     * apply them, auto-grade, and update status.
     */
    private void doAutoSubmit(ExamSubmission submission) {
        log.info("自动提交考试, submissionId: {}", submission.getId());

        // Load saved answers from Redis
        String progressKey = REDIS_PROGRESS_KEY + submission.getId();
        Map<Object, Object> redisAnswers = redisTemplate.opsForHash().entries(progressKey);

        // Apply any unsaved answers from Redis to DB
        if (redisAnswers != null && !redisAnswers.isEmpty()) {
            for (Map.Entry<Object, Object> entry : redisAnswers.entrySet()) {
                Long questionId = Long.parseLong(entry.getKey().toString());
                String answer = entry.getValue().toString();
                SubmissionAnswer sa = submissionAnswerMapper.selectBySubmissionAndQuestion(
                        submission.getId(), questionId);
                if (sa != null) {
                    submissionAnswerMapper.updateAnswer(sa.getId(), answer, LocalDateTime.now());
                }
            }
        }

        // Auto-grade all answers
        List<SubmissionAnswer> allAnswers = submissionAnswerMapper.selectBySubmissionId(submission.getId());
        BigDecimal totalScore = autoGradeAnswers(allAnswers);

        // Update submission status
        examSubmissionMapper.updateStatus(submission.getId(),
                SubmissionStatus.AUTO_SUBMITTED.name(), LocalDateTime.now(), totalScore);

        // Clean up Redis
        redisTemplate.delete(progressKey);
        redisTemplate.delete(REDIS_START_KEY + submission.getId());

        log.info("自动提交完成, submissionId: {}, totalScore: {}", submission.getId(), totalScore);
    }
}
