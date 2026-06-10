package com.training.controller;

import com.training.dto.response.CourseProgressDTO;
import com.training.dto.response.Result;
import com.training.entity.LearningRecord;
import com.training.security.CustomUserDetails;
import com.training.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@Slf4j
public class LearningRecordController {

    private final LearningRecordService learningRecordService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @PostMapping("/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<LearningRecord> updateProgress(
            @RequestParam Long coursewareId,
            @RequestParam Integer progress,
            @RequestParam(required = false) Integer lastPosition,
            @RequestParam(required = false) Integer studyDuration) {
        Long studentId = getCurrentUserId();
        log.info("更新学习进度: studentId={}, coursewareId={}, progress={}", studentId, coursewareId, progress);
        LearningRecord record = learningRecordService.updateProgress(studentId, coursewareId, progress, lastPosition, studyDuration);
        return Result.success(record);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<LearningRecord>> getCourseRecords(@PathVariable Long courseId) {
        Long studentId = getCurrentUserId();
        log.info("查询课程学习记录: studentId={}, courseId={}", studentId, courseId);
        List<LearningRecord> records = learningRecordService.getRecordsByCourse(studentId, courseId);
        return Result.success(records);
    }

    @GetMapping("/progress/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CourseProgressDTO> getCourseProgress(@PathVariable Long courseId) {
        Long studentId = getCurrentUserId();
        log.info("查询课程学习进度: studentId={}, courseId={}", studentId, courseId);
        CourseProgressDTO progress = learningRecordService.getCourseProgress(studentId, courseId);
        return Result.success(progress);
    }
}
