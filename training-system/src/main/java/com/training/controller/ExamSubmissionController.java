package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.AnswerSubmitRequest;
import com.training.dto.request.HeartbeatRequest;
import com.training.dto.request.SaveAnswerRequest;
import com.training.dto.response.ExamStartResponse;
import com.training.dto.response.Result;
import com.training.entity.ExamSubmission;
import com.training.entity.SubmissionAnswer;
import com.training.security.CustomUserDetails;
import com.training.service.ExamSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-submissions")
@RequiredArgsConstructor
@Slf4j
public class ExamSubmissionController {

    private final ExamSubmissionService examSubmissionService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @PostMapping("/start/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<ExamStartResponse> startExam(@PathVariable Long examId) {
        Long studentId = getCurrentUserId();
        log.info("开始考试: studentId={}, examId={}", studentId, examId);
        ExamStartResponse response = examSubmissionService.startExam(studentId, examId);
        return Result.success(response);
    }

    @PostMapping("/save-answer")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> saveAnswer(@RequestBody @Valid SaveAnswerRequest request) {
        Long studentId = getCurrentUserId();
        log.info("保存答案: studentId={}, submissionId={}", studentId, request.getSubmissionId());
        examSubmissionService.saveAnswer(request, studentId);
        return Result.success();
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> submitExam(@RequestBody @Valid AnswerSubmitRequest request) {
        Long studentId = getCurrentUserId();
        log.info("提交考试: studentId={}, submissionId={}", studentId, request.getSubmissionId());
        Map<String, Object> result = examSubmissionService.submitExam(request, studentId);
        return Result.success(result);
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> heartbeat(@RequestBody HeartbeatRequest request) {
        Long studentId = getCurrentUserId();
        log.info("考试心跳: studentId={}, submissionId={}", studentId, request.getSubmissionId());
        examSubmissionService.handleHeartbeat(request, studentId);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ExamSubmission> getSubmission(@PathVariable Long id) {
        log.info("查询考试提交详情: id={}", id);
        ExamSubmission submission = examSubmissionService.getSubmission(id, getCurrentUserId());
        return Result.success(submission);
    }

    @GetMapping("/{id}/answers")
    public Result<List<SubmissionAnswer>> getAnswers(@PathVariable Long id) {
        log.info("查询考试提交答案: submissionId={}", id);
        List<SubmissionAnswer> answers = examSubmissionService.getSubmissionAnswers(id);
        return Result.success(answers);
    }
}
