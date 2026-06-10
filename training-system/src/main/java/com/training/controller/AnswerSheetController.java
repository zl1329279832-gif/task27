package com.training.controller;

import com.training.common.Result;
import com.training.security.CustomUserDetails;
import com.training.service.AnswerSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answer-sheets")
@RequiredArgsConstructor
public class AnswerSheetController {

    private final AnswerSheetService answerSheetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long studentId) {
        return Result.success(answerSheetService.list(page, size, examId, studentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> getDetail(@PathVariable Long id) {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return Result.success(answerSheetService.getDetail(id, user.getUserId(), user.getRole()));
    }

    @PostMapping("/{id}/regrade")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> regrade(@PathVariable Long id) {
        Long graderId = getCurrentUserId();
        return Result.success(answerSheetService.regrade(id, graderId));
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
