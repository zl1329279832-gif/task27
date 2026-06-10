package com.training.controller;

import com.training.common.Result;
import com.training.entity.MakeupExamTask;
import com.training.service.MakeupExamTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/makeup-exam-tasks")
@RequiredArgsConstructor
public class MakeupExamTaskController {

    private final MakeupExamTaskService makeupExamTaskService;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<List<MakeupExamTask>> listByStudent(@PathVariable Long studentId,
                                                       @RequestParam(required = false) String status) {
        return Result.success(makeupExamTaskService.listByStudent(studentId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<MakeupExamTask> getById(@PathVariable Long id) {
        return Result.success(makeupExamTaskService.getById(id));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> schedule(@PathVariable Long id,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt) {
        makeupExamTaskService.schedule(id, scheduledAt);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> complete(@PathVariable Long id, @RequestParam Long answerSheetId) {
        makeupExamTaskService.complete(id, answerSheetId);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> cancel(@PathVariable Long id) {
        makeupExamTaskService.cancel(id);
        return Result.success();
    }
}
