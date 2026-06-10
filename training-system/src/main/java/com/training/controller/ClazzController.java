package com.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.common.Result;
import com.training.entity.Clazz;
import com.training.entity.Course;
import com.training.entity.SysUser;
import com.training.entity.dto.ClazzRequest;
import com.training.service.ClazzService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
public class ClazzController {

    private final ClazzService clazzService;

    @GetMapping
    public Result<IPage<Clazz>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(clazzService.list(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Clazz> getById(@PathVariable Long id) {
        return Result.success(clazzService.getById(id));
    }

    @PostMapping
    public Result<Clazz> create(@Valid @RequestBody ClazzRequest req) {
        return Result.success(clazzService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Clazz> update(@PathVariable Long id, @Valid @RequestBody ClazzRequest req) {
        return Result.success(clazzService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        clazzService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/students")
    public Result<Void> addStudent(@PathVariable Long id, @RequestParam Long studentId) {
        clazzService.addStudent(id, studentId);
        return Result.success();
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        clazzService.removeStudent(id, studentId);
        return Result.success();
    }

    @PostMapping("/{id}/courses")
    public Result<Void> addCourse(
            @PathVariable Long id,
            @RequestParam Long courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        clazzService.addCourse(id, courseId, startDate, endDate);
        return Result.success();
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    public Result<Void> removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        clazzService.removeCourse(id, courseId);
        return Result.success();
    }

    @GetMapping("/{id}/students")
    public Result<List<SysUser>> listStudents(@PathVariable Long id) {
        return Result.success(clazzService.listStudents(id));
    }

    @GetMapping("/{id}/courses")
    public Result<List<Course>> listCourses(@PathVariable Long id) {
        return Result.success(clazzService.listCourses(id));
    }
}
