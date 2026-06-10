package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.ChapterRequest;
import com.training.dto.response.Result;
import com.training.entity.Chapter;
import com.training.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
@Slf4j
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping
    public Result<List<Chapter>> listByCourse(@RequestParam Long courseId) {
        log.info("查询章节列表: courseId={}", courseId);
        List<Chapter> chapters = chapterService.listByCourseId(courseId);
        return Result.success(chapters);
    }

    @GetMapping("/{id}")
    public Result<Chapter> getById(@PathVariable Long id) {
        log.info("查询章节详情: id={}", id);
        Chapter chapter = chapterService.getChapterById(id);
        return Result.success(chapter);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Chapter> create(@RequestBody @Valid ChapterRequest request) {
        log.info("创建章节: title={}", request.getTitle());
        Chapter chapter = chapterService.createChapter(request);
        return Result.success(chapter);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ChapterRequest request) {
        log.info("更新章节: id={}", id);
        chapterService.updateChapter(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除章节: id={}", id);
        chapterService.deleteChapter(id);
        return Result.success();
    }
}
