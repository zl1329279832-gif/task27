package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.KnowledgePointRequest;
import com.training.service.KnowledgePointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> create(@Valid @RequestBody KnowledgePointRequest req) {
        return Result.success(knowledgePointService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody KnowledgePointRequest req) {
        return Result.success(knowledgePointService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> delete(@PathVariable Long id) {
        knowledgePointService.delete(id);
        return Result.success();
    }

    @GetMapping("/course/{courseId}")
    public Result<?> listByCourse(@PathVariable Long courseId) {
        return Result.success(knowledgePointService.listByCourse(courseId));
    }

    @GetMapping("/chapter/{chapterId}")
    public Result<?> listByChapter(@PathVariable Long chapterId) {
        return Result.success(knowledgePointService.listByChapter(chapterId));
    }

    @PostMapping("/question/{questionId}/link")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> linkToQuestion(@PathVariable Long questionId, @RequestBody List<Long> kpIds) {
        knowledgePointService.linkQuestionToKnowledgePoints(questionId, kpIds);
        return Result.success();
    }

    @GetMapping("/mastery/{studentId}/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<?> getMastery(@PathVariable Long studentId, @PathVariable Long courseId) {
        return Result.success(knowledgePointService.getStudentMastery(studentId, courseId));
    }
}
