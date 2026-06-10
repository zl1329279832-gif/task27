package com.training.controller;

import com.training.common.Result;
import com.training.entity.KnowledgeMastery;
import com.training.entity.dto.KnowledgeMasteryDTO;
import com.training.service.KnowledgeMasteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mastery")
@RequiredArgsConstructor
public class KnowledgeMasteryController {

    private final KnowledgeMasteryService knowledgeMasteryService;

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<List<KnowledgeMasteryDTO>> getMastery(@PathVariable Long studentId,
                                                         @PathVariable Long courseId) {
        return Result.success(knowledgeMasteryService.getMasteryByCourse(studentId, courseId));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<List<KnowledgeMastery>> evaluate(@RequestParam Long studentId,
                                                    @RequestParam Long courseId) {
        return Result.success(knowledgeMasteryService.evaluateMastery(studentId, courseId));
    }
}
