package com.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.common.Result;
import com.training.entity.LearningPath;
import com.training.entity.dto.LearningPathDTO;
import com.training.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<LearningPath> generate(@RequestParam Long studentId,
                                          @RequestParam Long courseId,
                                          @RequestParam(defaultValue = "MANUAL") String reason) {
        return Result.success(learningPathService.generatePath(studentId, courseId, reason));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<LearningPathDTO> getActivePath(@PathVariable Long studentId,
                                                  @PathVariable Long courseId) {
        return Result.success(learningPathService.getActivePath(studentId, courseId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<IPage<LearningPath>> listByStudent(@PathVariable Long studentId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(learningPathService.listByStudent(studentId, page, size));
    }
}
