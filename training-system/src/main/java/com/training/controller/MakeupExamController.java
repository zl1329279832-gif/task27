package com.training.controller;

import com.training.common.Result;
import com.training.security.CustomUserDetails;
import com.training.service.MakeupExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/makeup-exams")
@RequiredArgsConstructor
public class MakeupExamController {

    private final MakeupExamService makeupExamService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> listOwn(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long studentId = getCurrentUserId();
        return Result.success(makeupExamService.listByStudent(studentId, page, size));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(makeupExamService.listByStudent(studentId, page, size));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(makeupExamService.listByStudent(null, 1, 1));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> start(@PathVariable Long id) {
        makeupExamService.startMakeupExam(id);
        return Result.success();
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> recordResult(@PathVariable Long id, @RequestParam double score) {
        makeupExamService.recordMakeupExamResult(id, score);
        return Result.success();
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
