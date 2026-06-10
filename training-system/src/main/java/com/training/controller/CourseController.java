package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.CourseRequest;
import com.training.dto.response.PageResult;
import com.training.dto.response.Result;
import com.training.entity.Course;
import com.training.security.CustomUserDetails;
import com.training.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @GetMapping
    public Result<PageResult<Course>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询课程列表: keyword={}, status={}, page={}, size={}", keyword, status, page, size);
        PageResult<Course> result = courseService.listCourses(keyword, status, null, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Course> getById(@PathVariable Long id) {
        log.info("查询课程详情: id={}", id);
        Course course = courseService.getCourseById(id);
        return Result.success(course);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Course> create(@RequestBody @Valid CourseRequest request) {
        Long instructorId = getCurrentUserId();
        log.info("创建课程: title={}, instructorId={}", request.getTitle(), instructorId);
        Course course = courseService.createCourse(request, instructorId);
        return Result.success(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid CourseRequest request) {
        log.info("更新课程: id={}", id);
        courseService.updateCourse(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除课程: id={}", id);
        courseService.deleteCourse(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> publish(@PathVariable Long id) {
        log.info("发布课程: id={}", id);
        courseService.publishCourse(id);
        return Result.success();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> archive(@PathVariable Long id) {
        log.info("归档课程: id={}", id);
        courseService.archiveCourse(id);
        return Result.success();
    }
}
