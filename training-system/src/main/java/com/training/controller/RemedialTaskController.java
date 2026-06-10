package com.training.controller;

import com.training.common.Result;
import com.training.security.CustomUserDetails;
import com.training.service.RemedialTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/remedial-tasks")
@RequiredArgsConstructor
public class RemedialTaskController {

    private final RemedialTaskService remedialTaskService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> listOwn(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long studentId = getCurrentUserId();
        return Result.success(remedialTaskService.listByStudent(studentId, page, size));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(remedialTaskService.listByStudent(studentId, page, size));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(remedialTaskService.listByStudent(null, 1, 1));
    }

    @PostMapping("/{id}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> markProgress(@PathVariable Long id, @RequestParam double progress) {
        remedialTaskService.markProgress(id, progress);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> complete(@PathVariable Long id) {
        remedialTaskService.complete(id);
        return Result.success();
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
