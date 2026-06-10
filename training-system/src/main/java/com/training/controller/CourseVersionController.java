package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.CourseVersionUpgradeRequest;
import com.training.security.CustomUserDetails;
import com.training.service.CourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseVersionController {

    private final CourseVersionService courseVersionService;

    @PostMapping("/{id}/upgrade-version")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> upgradeVersion(
            @PathVariable Long id,
            @RequestBody(required = false) CourseVersionUpgradeRequest req) {
        Long operatorId = getCurrentUserId();
        String changeSummary = req != null ? req.getChangeSummary() : null;
        return Result.success(courseVersionService.upgradeVersion(id, changeSummary, operatorId));
    }

    @GetMapping("/{id}/version-history")
    public Result<?> getVersionHistory(@PathVariable Long id) {
        return Result.success(courseVersionService.getVersionHistory(id));
    }

    @GetMapping("/{id}/current-version")
    public Result<?> getCurrentVersion(@PathVariable Long id) {
        return Result.success(courseVersionService.getCurrentVersion(id));
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
