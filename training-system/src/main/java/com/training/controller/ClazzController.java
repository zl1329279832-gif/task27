package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.ClazzRequest;
import com.training.dto.response.PageResult;
import com.training.dto.response.Result;
import com.training.entity.Clazz;
import com.training.entity.ClazzStudent;
import com.training.service.ClazzService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clazzes")
@RequiredArgsConstructor
@Slf4j
public class ClazzController {

    private final ClazzService clazzService;

    @GetMapping
    public Result<PageResult<Clazz>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询班级列表: keyword={}, status={}, courseId={}, page={}, size={}", keyword, status, courseId, page, size);
        PageResult<Clazz> result = clazzService.listClazzes(keyword, status, courseId, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Clazz> getById(@PathVariable Long id) {
        log.info("查询班级详情: id={}", id);
        Clazz clazz = clazzService.getClazzById(id);
        return Result.success(clazz);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Clazz> create(@RequestBody @Valid ClazzRequest request) {
        log.info("创建班级: name={}", request.getName());
        Clazz clazz = clazzService.createClazz(request);
        return Result.success(clazz);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ClazzRequest request) {
        log.info("更新班级: id={}", id);
        clazzService.updateClazz(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除班级: id={}", id);
        clazzService.deleteClazz(id);
        return Result.success();
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> addStudent(@PathVariable Long id, @RequestParam Long studentId) {
        log.info("添加学生到班级: clazzId={}, studentId={}", id, studentId);
        clazzService.addStudent(id, studentId);
        return Result.success();
    }

    @DeleteMapping("/{id}/students/{sid}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> removeStudent(@PathVariable Long id, @PathVariable Long sid) {
        log.info("移除班级学生: clazzId={}, studentId={}", id, sid);
        clazzService.removeStudent(id, sid);
        return Result.success();
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<List<ClazzStudent>> listStudents(@PathVariable Long id) {
        log.info("查询班级学生: clazzId={}", id);
        List<ClazzStudent> students = clazzService.listStudents(id);
        return Result.success(students);
    }
}
