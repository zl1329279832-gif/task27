package com.training.controller;

import com.training.common.Result;
import com.training.entity.Exam;
import com.training.entity.AnswerSheet;
import com.training.entity.dto.ExamRequest;
import com.training.entity.dto.ExamStartResponse;
import com.training.entity.dto.AnswerSubmitRequest;
import com.training.security.CustomUserDetails;
import com.training.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status) {
        return Result.success(examService.list(page, size, courseId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(examService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> create(@Valid @RequestBody ExamRequest req) {
        Long userId = getCurrentUserId();
        return Result.success(examService.create(req, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ExamRequest req) {
        return Result.success(examService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> delete(@PathVariable Long id) {
        examService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> publish(@PathVariable Long id) {
        examService.publish(id);
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> close(@PathVariable Long id) {
        examService.close(id);
        return Result.success();
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> startExam(@PathVariable Long id) {
        Long studentId = getCurrentUserId();
        return Result.success(examService.startExam(id, studentId));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> submitExam(@Valid @RequestBody AnswerSubmitRequest req) {
        Long studentId = getCurrentUserId();
        return Result.success(examService.submitExam(req, studentId));
    }

    @PostMapping("/{id}/report-tab-switch")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> reportTabSwitch(@PathVariable Long id) {
        Long studentId = getCurrentUserId();
        examService.reportTabSwitch(id, studentId);
        return Result.success();
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
