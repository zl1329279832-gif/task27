package com.training.controller;

import com.training.dto.response.Result;
import com.training.entity.Score;
import com.training.security.CustomUserDetails;
import com.training.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@Slf4j
public class ScoreController {

    private final ScoreService scoreService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @GetMapping("/student/{studentId}")
    public Result<List<Score>> getByStudent(@PathVariable Long studentId) {
        log.info("查询学生成绩: studentId={}", studentId);
        List<Score> scores = scoreService.getScoresByStudent(studentId);
        return Result.success(scores);
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','AUDITOR')")
    public Result<List<Score>> getByExam(@PathVariable Long examId) {
        log.info("查询考试成绩: examId={}", examId);
        List<Score> scores = scoreService.getScoresByExam(examId);
        return Result.success(scores);
    }

    @GetMapping("/{id}")
    public Result<Score> getById(@PathVariable Long id) {
        log.info("查询成绩详情: id={}", id);
        Score score = scoreService.getScoreById(id);
        return Result.success(score);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<Void> approve(@PathVariable Long id) {
        log.info("审核通过成绩: id={}", id);
        scoreService.approveScore(id, getCurrentUserId());
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<Void> reject(@PathVariable Long id) {
        log.info("审核拒绝成绩: id={}", id);
        scoreService.rejectScore(id, getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Score>> getMyScores() {
        Long studentId = getCurrentUserId();
        log.info("查询我的成绩: studentId={}", studentId);
        List<Score> scores = scoreService.getScoresByStudent(studentId);
        return Result.success(scores);
    }
}
