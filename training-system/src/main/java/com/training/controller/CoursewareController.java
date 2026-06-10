package com.training.controller;

import com.training.common.Result;
import com.training.entity.Courseware;
import com.training.entity.dto.CoursewareRequest;
import com.training.service.CoursewareService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CoursewareController {

    private final CoursewareService coursewareService;

    @GetMapping("/api/chapters/{chapterId}/coursewares")
    public Result<List<Courseware>> list(@PathVariable Long chapterId) {
        return Result.success(coursewareService.listByChapterId(chapterId));
    }

    @PostMapping("/api/chapters/{chapterId}/coursewares")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Courseware> create(@PathVariable Long chapterId,
                                     @RequestPart("data") CoursewareRequest req,
                                     @RequestPart(value = "file", required = false) MultipartFile file) {
        return Result.success(coursewareService.create(chapterId, req, file));
    }

    @PutMapping("/api/coursewares/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Courseware> update(@PathVariable Long id,
                                     @RequestBody CoursewareRequest req) {
        return Result.success(coursewareService.update(id, req));
    }

    @DeleteMapping("/api/coursewares/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        coursewareService.delete(id);
        return Result.success();
    }

    @GetMapping("/api/coursewares/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = coursewareService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
