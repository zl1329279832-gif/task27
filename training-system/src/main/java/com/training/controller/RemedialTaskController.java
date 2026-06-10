package com.training.controller;

import com.training.common.Result;
import com.training.entity.RemedialTask;
import com.training.service.RemedialTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/remedial-tasks")
@RequiredArgsConstructor
public class RemedialTaskController {

    private final RemedialTaskService remedialTaskService;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<List<RemedialTask>> listByStudent(@PathVariable Long studentId,
                                                     @RequestParam(required = false) String status) {
        return Result.success(remedialTaskService.listByStudent(studentId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<RemedialTask> getById(@PathVariable Long id) {
        return Result.success(remedialTaskService.getById(id));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> start(@PathVariable Long id) {
        remedialTaskService.start(id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> complete(@PathVariable Long id) {
        remedialTaskService.complete(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> cancel(@PathVariable Long id) {
        remedialTaskService.cancel(id);
        return Result.success();
    }
}
