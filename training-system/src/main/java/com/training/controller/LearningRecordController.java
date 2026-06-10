package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.ChapterProgressDTO;
import com.training.entity.dto.CourseProgressDTO;
import com.training.entity.dto.HeartbeatRequest;
import com.training.security.CustomUserDetails;
import com.training.service.LearningRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningRecordController {

    private final LearningRecordService learningRecordService;

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> heartbeat(@Valid @RequestBody HeartbeatRequest req) {
        Long userId = getCurrentUserId();
        learningRecordService.heartbeat(userId, req);
        return Result.success();
    }

    @GetMapping("/course/{courseId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<CourseProgressDTO> getCourseProgress(@PathVariable Long courseId) {
        Long userId = getCurrentUserId();
        CourseProgressDTO progress = learningRecordService.getCourseProgress(userId, courseId);
        return Result.success(progress);
    }

    @GetMapping("/course/{courseId}/chapters")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<List<ChapterProgressDTO>> getChapterProgress(@PathVariable Long courseId) {
        Long userId = getCurrentUserId();
        List<ChapterProgressDTO> chapters = learningRecordService.getChapterProgress(userId, courseId);
        return Result.success(chapters);
    }

    @GetMapping("/student/{studentId}/course/{courseId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','AUDITOR')")
    public Result<CourseProgressDTO> getStudentCourseProgress(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        CourseProgressDTO progress = learningRecordService.getCourseProgress(studentId, courseId);
        return Result.success(progress);
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
