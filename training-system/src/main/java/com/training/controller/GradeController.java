package com.training.controller;

import com.training.common.Result;
import com.training.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(gradeService.listByStudent(studentId, page, size));
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','AUDITOR')")
    public Result<?> listByExam(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(gradeService.listByExam(examId, page, size));
    }

    @GetMapping("/course/{courseId}/stats")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','AUDITOR')")
    public Result<?> getCourseStats(@PathVariable Long courseId) {
        return Result.success(gradeService.getCourseStats(courseId));
    }

    @GetMapping("/answer-sheet/{answerSheetId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> getByAnswerSheet(@PathVariable Long answerSheetId) {
        return Result.success(gradeService.getByAnswerSheet(answerSheetId));
    }
}
