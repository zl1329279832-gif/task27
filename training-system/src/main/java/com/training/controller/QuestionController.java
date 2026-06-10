package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.QuestionRequest;
import com.training.security.CustomUserDetails;
import com.training.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','AUDITOR')")
    public Result<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String difficulty) {
        return Result.success(questionService.list(page, size, courseId, questionType, difficulty));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','AUDITOR')")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(questionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> create(@Valid @RequestBody QuestionRequest req) {
        Long userId = getCurrentUserId();
        return Result.success(questionService.create(req, userId));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> batchCreate(@Valid @RequestBody List<QuestionRequest> requests) {
        Long userId = getCurrentUserId();
        questionService.batchCreate(requests, userId);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody QuestionRequest req) {
        return Result.success(questionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
