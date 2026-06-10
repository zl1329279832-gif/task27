package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.LearningPathGenerateRequest;
import com.training.security.CustomUserDetails;
import com.training.service.LearningPathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> generate(@Valid @RequestBody LearningPathGenerateRequest req) {
        Long operatorId = getCurrentUserId();
        return Result.success(learningPathService.generatePath(
                req.getStudentId(), req.getCourseId(),
                req.getTriggerReason(), operatorId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> getActivePath(
            @RequestParam Long courseId) {
        Long studentId = getCurrentUserId();
        return Result.success(learningPathService.getActivePath(studentId, courseId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<?> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(learningPathService.listPaths(studentId, page, size));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(learningPathService.listPaths(null, 1, 1));
    }

    @PostMapping("/{id}/steps/{step}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> completeStep(@PathVariable Long id, @PathVariable Integer step) {
        learningPathService.completePathStep(id, step);
        return Result.success();
    }

    @PostMapping("/{id}/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> expirePath(@PathVariable Long id) {
        learningPathService.expirePath(id);
        return Result.success();
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
