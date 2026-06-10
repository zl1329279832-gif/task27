package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.*;
import com.training.mapper.*;
import com.training.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final GradeMapper gradeMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Exam create(ExamRequest req, Long instructorId) {
        Exam exam = Exam.builder()
                .courseId(req.getCourseId())
                .title(req.getTitle())
                .description(req.getDescription())
                .durationMinutes(req.getDurationMinutes())
                .totalScore(req.getTotalScore() != null ? req.getTotalScore() : 100)
                .passScore(req.getPassScore() != null ? req.getPassScore() : 60)
                .questionCount(req.getQuestionCount() != null ? req.getQuestionCount() : 0)
                .randomize(req.getRandomize() != null && req.getRandomize() ? 1 : 0)
                .maxAttempts(req.getMaxAttempts() != null ? req.getMaxAttempts() : 3)
                .antiCheatEnabled(req.getAntiCheatEnabled() != null && req.getAntiCheatEnabled() ? 1 : 0)
                .maxTabSwitches(req.getMaxTabSwitches() != null ? req.getMaxTabSwitches() : 3)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status("DRAFT")
                .build();
        examMapper.insert(exam);

        // Add questions to exam
        if (req.getQuestions() != null) {
            for (ExamRequest.ExamQuestionItem item : req.getQuestions()) {
                ExamQuestion eq = ExamQuestion.builder()
                        .examId(exam.getId())
                        .questionId(item.getQuestionId())
                        .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                        .scoreOverride(item.getScoreOverride())
                        .build();
                examQuestionMapper.insert(eq);
            }
            exam.setQuestionCount(req.getQuestions().size());
            examMapper.updateById(exam);
        }

        return exam;
    }

    @Override
    public Exam update(Long id, ExamRequest req) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"DRAFT".equals(exam.getStatus())) throw new BusinessException("只能修改草稿状态的考试");

        exam.setTitle(req.getTitle() != null ? req.getTitle() : exam.getTitle());
        exam.setDescription(req.getDescription() != null ? req.getDescription() : exam.getDescription());
        exam.setDurationMinutes(req.getDurationMinutes() != null ? req.getDurationMinutes() : exam.getDurationMinutes());
        exam.setPassScore(req.getPassScore() != null ? req.getPassScore() : exam.getPassScore());
        exam.setMaxAttempts(req.getMaxAttempts() != null ? req.getMaxAttempts() : exam.getMaxAttempts());
        exam.setStartTime(req.getStartTime() != null ? req.getStartTime() : exam.getStartTime());
        exam.setEndTime(req.getEndTime() != null ? req.getEndTime() : exam.getEndTime());
        examMapper.updateById(exam);

        // Rebuild questions if provided
        if (req.getQuestions() != null) {
            examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                    .eq(ExamQuestion::getExamId, id));
            for (ExamRequest.ExamQuestionItem item : req.getQuestions()) {
                ExamQuestion eq = ExamQuestion.builder()
                        .examId(id)
                        .questionId(item.getQuestionId())
                        .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                        .scoreOverride(item.getScoreOverride())
                        .build();
                examQuestionMapper.insert(eq);
            }
            exam.setQuestionCount(req.getQuestions().size());
            examMapper.updateById(exam);
        }

        return exam;
    }

    @Override
    public Exam getById(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        return exam;
    }

    @Override
    public IPage<Exam> list(int page, int size, Long courseId, String status) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) wrapper.eq(Exam::getCourseId, courseId);
        if (status != null) wrapper.eq(Exam::getStatus, status);
        wrapper.orderByDesc(Exam::getCreatedAt);
        return examMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void delete(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"DRAFT".equals(exam.getStatus())) throw new BusinessException("只能删除草稿状态的考试");
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id));
        examMapper.deleteById(id);
    }

    @Override
    public void publish(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"DRAFT".equals(exam.getStatus())) throw new BusinessException("只能发布草稿状态的考试");

        // Verify questions exist
        long qCount = examQuestionMapper.selectCount(
                new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        if (qCount == 0) throw new BusinessException("考试至少需要包含一道题目");

        exam.setStatus("PUBLISHED");
        examMapper.updateById(exam);
    }

    @Override
    public void close(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        exam.setStatus("CLOSED");
        examMapper.updateById(exam);
    }

    @Override
    @Transactional
    public ExamStartResponse startExam(Long examId, Long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"PUBLISHED".equals(exam.getStatus())) throw new BusinessException("考试未发布");

        // Check time window
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime()))
            throw new BusinessException("考试尚未开始");
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime()))
            throw new BusinessException("考试已结束");

        // Check attempt count
        long attemptCount = answerSheetMapper.selectCount(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getExamId, examId)
                        .eq(AnswerSheet::getStudentId, studentId)
                        .ne(AnswerSheet::getStatus, "IN_PROGRESS"));
        if (attemptCount >= exam.getMaxAttempts())
            throw new BusinessException("已达到最大考试次数: " + exam.getMaxAttempts());

        // Check for in-progress answer sheet (resume/breakpoint)
        AnswerSheet existingSheet = answerSheetMapper.selectOne(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getExamId, examId)
                        .eq(AnswerSheet::getStudentId, studentId)
                        .eq(AnswerSheet::getStatus, "IN_PROGRESS"));

        if (existingSheet != null) {
            // Resume exam - use frozen snapshot, NOT live question data
            return buildResumeResponse(existingSheet, exam);
        }

        // Create new attempt
        int attemptNo = (int) attemptCount + 1;
        int remainingSeconds = exam.getDurationMinutes() * 60;

        AnswerSheet sheet = AnswerSheet.builder()
                .examId(examId)
                .studentId(studentId)
                .attemptNo(attemptNo)
                .status("IN_PROGRESS")
                .startTime(now)
                .remainingSeconds(remainingSeconds)
                .tabSwitchCount(0)
                .build();
        answerSheetMapper.insert(sheet);

        // Set Redis timeout key
        String timeoutKey = "exam:timeout:" + sheet.getId();
        redisTemplate.opsForValue().set(timeoutKey, "1", remainingSeconds, TimeUnit.SECONDS);

        // Build question list and freeze snapshot
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSortOrder));

        // Handle randomization
        if (exam.getRandomize() == 1 && exam.getQuestionCount() > 0
                && exam.getQuestionCount() < examQuestions.size()) {
            Collections.shuffle(examQuestions);
            examQuestions = examQuestions.subList(0, exam.getQuestionCount());
        }

        List<AnswerSheet.QuestionSnapshot> snapshots = new ArrayList<>();
        List<ExamStartResponse.QuestionItem> questionItems = new ArrayList<>();

        for (ExamQuestion eq : examQuestions) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q == null || "DELETED".equals(q.getStatus())) continue;

            int qScore = eq.getScoreOverride() != null ? eq.getScoreOverride() : q.getScore();

            // Freeze question data into snapshot
            AnswerSheet.QuestionSnapshot snapshot = AnswerSheet.QuestionSnapshot.builder()
                    .questionId(q.getId())
                    .examQuestionId(eq.getId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .options(q.getOptions())
                    .correctAnswer(q.getCorrectAnswer())
                    .score(qScore)
                    .sortOrder(eq.getSortOrder())
                    .build();
            snapshots.add(snapshot);

            questionItems.add(ExamStartResponse.QuestionItem.builder()
                    .examQuestionId(eq.getId())
                    .questionId(q.getId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .options(q.getOptions())
                    .score(qScore)
                    .sortOrder(eq.getSortOrder())
                    .build());

            // Create answer detail with frozen correctAnswer and score
            AnswerDetail detail = AnswerDetail.builder()
                    .answerSheetId(sheet.getId())
                    .questionId(q.getId())
                    .examQuestionId(eq.getId())
                    .correctAnswer(q.getCorrectAnswer())
                    .snapshotScore(qScore)
                    .build();
            answerDetailMapper.insert(detail);
        }

        // Persist the frozen snapshot on the answer sheet
        sheet.setQuestionSnapshot(snapshots);
        answerSheetMapper.updateById(sheet);

        return ExamStartResponse.builder()
                .answerSheetId(sheet.getId())
                .attemptNo(attemptNo)
                .remainingSeconds(remainingSeconds)
                .totalQuestions(questionItems.size())
                .resumed(false)
                .questions(questionItems)
                .existingAnswers(Collections.emptyList())
                .build();
    }

    /**
     * Build resume response from the frozen snapshot stored on the answer sheet.
     * NEVER reads from the live question table — this prevents question bank changes
     * from affecting students who are resuming an exam after a disconnection.
     */
    private ExamStartResponse buildResumeResponse(AnswerSheet sheet, Exam exam) {
        // Get existing answers
        List<AnswerDetail> details = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>()
                        .eq(AnswerDetail::getAnswerSheetId, sheet.getId()));

        // Build a lookup map from snapshot (frozen data)
        Map<Long, AnswerSheet.QuestionSnapshot> snapshotMap = new HashMap<>();
        if (sheet.getQuestionSnapshot() != null) {
            for (AnswerSheet.QuestionSnapshot snap : sheet.getQuestionSnapshot()) {
                snapshotMap.put(snap.getQuestionId(), snap);
            }
        }

        List<ExamStartResponse.QuestionItem> questionItems = new ArrayList<>();
        List<ExamStartResponse.AnswerItem> answerItems = new ArrayList<>();

        for (AnswerDetail detail : details) {
            // Use snapshot data, NOT live question data
            AnswerSheet.QuestionSnapshot snap = snapshotMap.get(detail.getQuestionId());
            if (snap == null) {
                // Fallback: if somehow no snapshot exists (legacy data), skip
                log.warn("No snapshot found for questionId={} in answerSheet={}, skipping",
                        detail.getQuestionId(), sheet.getId());
                continue;
            }

            questionItems.add(ExamStartResponse.QuestionItem.builder()
                    .examQuestionId(snap.getExamQuestionId())
                    .questionId(snap.getQuestionId())
                    .content(snap.getContent())
                    .questionType(snap.getQuestionType())
                    .options(snap.getOptions())
                    .score(snap.getScore())
                    .sortOrder(snap.getSortOrder())
                    .build());

            if (detail.getStudentAnswer() != null) {
                answerItems.add(ExamStartResponse.AnswerItem.builder()
                        .examQuestionId(snap.getExamQuestionId())
                        .questionId(detail.getQuestionId())
                        .studentAnswer(detail.getStudentAnswer())
                        .build());
            }
        }

        // Calculate remaining time from Redis or DB
        Integer remaining = sheet.getRemainingSeconds();
        String timeoutKey = "exam:timeout:" + sheet.getId();
        Long redisTTL = redisTemplate.getExpire(timeoutKey, TimeUnit.SECONDS);
        if (redisTTL != null && redisTTL > 0) {
            remaining = redisTTL.intValue();
        } else if (redisTTL != null && redisTTL <= 0) {
            // Redis key expired — compute from DB startTime + durationMinutes
            if (sheet.getStartTime() != null) {
                long elapsed = java.time.Duration.between(sheet.getStartTime(), LocalDateTime.now()).getSeconds();
                long totalAllowed = (long) exam.getDurationMinutes() * 60;
                remaining = (int) Math.max(0, totalAllowed - elapsed);
            }
        }

        return ExamStartResponse.builder()
                .answerSheetId(sheet.getId())
                .attemptNo(sheet.getAttemptNo())
                .remainingSeconds(remaining)
                .totalQuestions(questionItems.size())
                .resumed(true)
                .questions(questionItems)
                .existingAnswers(answerItems)
                .build();
    }

    @Override
    @Transactional
    public AnswerSheet submitExam(AnswerSubmitRequest req, Long studentId) {
        AnswerSheet sheet = answerSheetMapper.selectById(req.getAnswerSheetId());
        if (sheet == null) throw new BusinessException("答卷不存在");
        if (!sheet.getStudentId().equals(studentId)) throw new BusinessException("无权操作此答卷");
        if (!"IN_PROGRESS".equals(sheet.getStatus()))
            throw new BusinessException("答卷已提交或已超时，不能重复提交");

        // Idempotent check via Redis — clean up on failure so student can retry
        String idempotentKey = "submit:" + sheet.getExamId() + ":" + studentId + ":" + sheet.getAttemptNo();
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 30, TimeUnit.SECONDS);
        if (isFirst != null && !isFirst) {
            throw new BusinessException("请勿重复提交答卷");
        }

        Exam exam;
        try {
            exam = examMapper.selectById(sheet.getExamId());

            // Save answers
            if (req.getAnswers() != null) {
                for (AnswerSubmitRequest.AnswerItem answerItem : req.getAnswers()) {
                    LambdaQueryWrapper<AnswerDetail> wrapper = new LambdaQueryWrapper<AnswerDetail>()
                            .eq(AnswerDetail::getAnswerSheetId, sheet.getId())
                            .eq(AnswerDetail::getQuestionId, answerItem.getQuestionId());
                    if (answerItem.getExamQuestionId() != null) {
                        wrapper.eq(AnswerDetail::getExamQuestionId, answerItem.getExamQuestionId());
                    }
                    AnswerDetail detail = answerDetailMapper.selectOne(wrapper);
                    if (detail != null) {
                        detail.setStudentAnswer(answerItem.getAnswer());
                        answerDetailMapper.updateById(detail);
                    }
                }
            }

            // Auto-grade using frozen snapshot data
            return autoGrade(sheet, exam, "SUBMITTED");
        } catch (Exception e) {
            // On grading failure, remove idempotent key so student can retry
            redisTemplate.delete(idempotentKey);
            throw e;
        }
    }

    /**
     * Auto-grade using frozen data from AnswerDetail (correctAnswer, snapshotScore).
     * Does NOT read from the live question table to ensure grading consistency
     * even if the question bank has been modified since the exam started.
     */
    private AnswerSheet autoGrade(AnswerSheet sheet, Exam exam, String submitStatus) {
        List<AnswerDetail> details = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>()
                        .eq(AnswerDetail::getAnswerSheetId, sheet.getId()));

        // Build snapshot lookup for fallback on legacy data
        Map<Long, AnswerSheet.QuestionSnapshot> snapshotMap = new HashMap<>();
        if (sheet.getQuestionSnapshot() != null) {
            for (AnswerSheet.QuestionSnapshot snap : sheet.getQuestionSnapshot()) {
                snapshotMap.put(snap.getQuestionId(), snap);
            }
        }

        double totalScore = 0;
        boolean allGraded = true;

        for (AnswerDetail detail : details) {
            // Use frozen data from AnswerDetail first, then snapshot fallback
            String correctAns = detail.getCorrectAnswer();
            int qScore = detail.getSnapshotScore() != null ? detail.getSnapshotScore() : 0;
            String questionType = null;

            AnswerSheet.QuestionSnapshot snap = snapshotMap.get(detail.getQuestionId());
            if (snap != null) {
                if (correctAns == null) correctAns = snap.getCorrectAnswer();
                if (detail.getSnapshotScore() == null) qScore = snap.getScore();
                questionType = snap.getQuestionType();
            }

            // If no snapshot data at all (legacy), fall back to live question table
            if (questionType == null) {
                Question question = questionMapper.selectById(detail.getQuestionId());
                if (question == null || "DELETED".equals(question.getStatus())) {
                    // Deleted question with no snapshot — give full score
                    if (qScore == 0) qScore = 10;
                    detail.setIsCorrect(1);
                    detail.setScoreEarned((double) qScore);
                    detail.setGradingNote("题目已删除，自动给满分");
                    answerDetailMapper.updateById(detail);
                    totalScore += qScore;
                    continue;
                }
                questionType = question.getQuestionType();
                if (correctAns == null) correctAns = question.getCorrectAnswer();
                if (qScore == 0) qScore = question.getScore();
            }

            if ("SHORT_ANSWER".equals(questionType)) {
                // Subjective — needs manual grading
                detail.setGradingNote("待人工批改");
                answerDetailMapper.updateById(detail);
                allGraded = false;
                continue;
            }

            // Auto-grade objective questions using frozen correctAnswer
            boolean isCorrect = false;
            if (detail.getStudentAnswer() != null && correctAns != null) {
                String studentAns = detail.getStudentAnswer().trim();
                String frozenCorrectAns = correctAns.trim();

                if ("MULTI_CHOICE".equals(questionType)) {
                    String[] sArr = studentAns.split("[,，、]");
                    String[] cArr = frozenCorrectAns.split("[,，、]");
                    Arrays.sort(sArr);
                    Arrays.sort(cArr);
                    isCorrect = Arrays.equals(sArr, cArr);
                } else {
                    isCorrect = studentAns.equalsIgnoreCase(frozenCorrectAns);
                }
            }

            detail.setIsCorrect(isCorrect ? 1 : 0);
            detail.setScoreEarned(isCorrect ? (double) qScore : 0.0);
            detail.setGradingNote("自动批改");
            answerDetailMapper.updateById(detail);

            if (isCorrect) totalScore += qScore;
        }

        // Update answer sheet
        sheet.setStatus(submitStatus);
        sheet.setSubmitTime(LocalDateTime.now());
        sheet.setRemainingSeconds(0);
        sheet.setScore(totalScore);
        sheet.setPass(totalScore >= exam.getPassScore() ? 1 : 0);

        if (allGraded) {
            sheet.setGradingCompletedAt(LocalDateTime.now());
        }
        answerSheetMapper.updateById(sheet);

        // Remove Redis timeout key
        redisTemplate.delete("exam:timeout:" + sheet.getId());

        // Create grade record
        if (allGraded) {
            Grade grade = Grade.builder()
                    .answerSheetId(sheet.getId())
                    .studentId(sheet.getStudentId())
                    .examId(sheet.getExamId())
                    .courseId(exam.getCourseId())
                    .score(totalScore)
                    .totalScore(exam.getTotalScore())
                    .pass(totalScore >= exam.getPassScore() ? 1 : 0)
                    .gradedBy(0L) // system auto
                    .gradedAt(LocalDateTime.now())
                    .build();
            gradeMapper.insert(grade);
        }

        return sheet;
    }

    @Override
    @Transactional
    public void reportTabSwitch(Long examId, Long studentId) {
        AnswerSheet sheet = answerSheetMapper.selectOne(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getExamId, examId)
                        .eq(AnswerSheet::getStudentId, studentId)
                        .eq(AnswerSheet::getStatus, "IN_PROGRESS"));
        if (sheet == null) return;

        Exam exam = examMapper.selectById(examId);
        sheet.setTabSwitchCount(sheet.getTabSwitchCount() + 1);
        answerSheetMapper.updateById(sheet);

        // Auto-submit if exceeds max tab switches
        if (exam.getAntiCheatEnabled() == 1
                && sheet.getTabSwitchCount() > exam.getMaxTabSwitches()) {
            autoGrade(sheet, exam, "AUTO_SUBMITTED");
        }
    }

    @Override
    @Transactional
    public void handleTimeout(Long answerSheetId) {
        AnswerSheet sheet = answerSheetMapper.selectById(answerSheetId);
        if (sheet == null || !"IN_PROGRESS".equals(sheet.getStatus())) return;

        Exam exam = examMapper.selectById(sheet.getExamId());
        autoGrade(sheet, exam, "TIMED_OUT");
    }
}
