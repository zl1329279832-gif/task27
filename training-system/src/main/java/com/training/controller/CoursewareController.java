package com.training.controller;

import com.training.dto.response.Result;
import com.training.entity.Courseware;
import com.training.service.CoursewareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/coursewares")
@RequiredArgsConstructor
@Slf4j
public class CoursewareController {

    private final CoursewareService coursewareService;

    @GetMapping
    public Result<List<Courseware>> listByChapter(@RequestParam Long chapterId) {
        log.info("查询课件列表: chapterId={}", chapterId);
        List<Courseware> coursewares = coursewareService.listByChapterId(chapterId);
        return Result.success(coursewares);
    }

    @GetMapping("/{id}")
    public Result<Courseware> getById(@PathVariable Long id) {
        log.info("查询课件详情: id={}", id);
        Courseware courseware = coursewareService.getCoursewareById(id);
        return Result.success(courseware);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Courseware> upload(
            @RequestParam Long chapterId,
            @RequestParam String title,
            @RequestParam String fileType,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) Integer duration) {
        log.info("上传课件: chapterId={}, title={}, fileType={}", chapterId, title, fileType);
        Courseware courseware = coursewareService.createCourseware(chapterId, title, fileType, file, sortOrder, duration);
        return Result.success(courseware);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) Integer duration) {
        log.info("更新课件: id={}, title={}", id, title);
        coursewareService.updateCourseware(id, title, sortOrder, duration);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除课件: id={}", id);
        coursewareService.deleteCourseware(id);
        return Result.success();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        log.info("下载课件: id={}", id);
        Courseware courseware = coursewareService.getCoursewareById(id);
        byte[] data = coursewareService.downloadCourseware(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + courseware.getTitle() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
