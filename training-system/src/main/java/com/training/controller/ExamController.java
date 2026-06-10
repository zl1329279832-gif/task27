package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.ExamRequest;
import com.training.dto.response.PageResult;
import com.training.dto.response.Result;
import com.training.entity.Exam;
import com.training.security.CustomUserDetails;
import com.training.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @GetMapping
    public Result<PageResult<Exam>> list(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询考试列表: courseId={}, status={}, keyword={}, page={}, size={}", courseId, status, keyword, page, size);
        PageResult<Exam> result = examService.listExams(courseId, status, keyword, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Exam> getById(@PathVariable Long id) {
        log.info("查询考试详情: id={}", id);
        Exam exam = examService.getExamById(id);
        return Result.success(exam);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Exam> create(@RequestBody @Valid ExamRequest request) {
        log.info("创建考试: title={}", request.getTitle());
        Exam exam = examService.createExam(request, getCurrentUserId());
        return Result.success(exam);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ExamRequest request) {
        log.info("更新考试: id={}", id);
        examService.updateExam(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除考试: id={}", id);
        examService.deleteExam(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> publish(@PathVariable Long id) {
        log.info("发布考试: id={}", id);
        examService.publishExam(id);
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> close(@PathVariable Long id) {
        log.info("关闭考试: id={}", id);
        examService.closeExam(id);
        return Result.success();
    }
}
