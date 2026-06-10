package com.training.controller;

import com.training.common.Result;
import com.training.entity.Chapter;
import com.training.entity.dto.ChapterRequest;
import com.training.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping
    public Result<List<Chapter>> list(@PathVariable Long courseId) {
        return Result.success(chapterService.listByCourseId(courseId));
    }

    @GetMapping("/{id}")
    public Result<Chapter> getById(@PathVariable Long id) {
        return Result.success(chapterService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Chapter> create(@PathVariable Long courseId,
                                  @Valid @RequestBody ChapterRequest req) {
        return Result.success(chapterService.create(courseId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Chapter> update(@PathVariable Long id,
                                  @Valid @RequestBody ChapterRequest req) {
        return Result.success(chapterService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        chapterService.delete(id);
        return Result.success();
    }

    @PutMapping("/sort")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> updateSort(@RequestBody List<Long> chapterIds) {
        chapterService.updateSort(chapterIds);
        return Result.success();
    }
}
