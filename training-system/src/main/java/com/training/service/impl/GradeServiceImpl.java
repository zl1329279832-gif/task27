package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.entity.Grade;
import com.training.mapper.GradeMapper;
import com.training.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeMapper gradeMapper;

    @Override
    public IPage<Grade> listByStudent(Long studentId, int page, int size) {
        return gradeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, studentId)
                        .orderByDesc(Grade::getGradedAt));
    }

    @Override
    public IPage<Grade> listByExam(Long examId, int page, int size) {
        return gradeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getExamId, examId)
                        .orderByDesc(Grade::getScore));
    }

    @Override
    public Map<String, Object> getCourseStats(Long courseId) {
        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getCourseId, courseId));

        Map<String, Object> stats = new HashMap<>();
        if (grades.isEmpty()) {
            stats.put("totalStudents", 0);
            stats.put("avgScore", 0);
            stats.put("maxScore", 0);
            stats.put("minScore", 0);
            stats.put("passRate", 0);
            return stats;
        }

        DoubleSummaryStatistics scoreStats = grades.stream()
                .filter(g -> g.getScore() != null)
                .mapToDouble(g -> g.getScore())
                .summaryStatistics();

        long passCount = grades.stream().filter(g -> g.getPass() != null && g.getPass() == 1).count();

        stats.put("totalStudents", grades.size());
        stats.put("avgScore", Math.round(scoreStats.getAverage() * 100.0) / 100.0);
        stats.put("maxScore", scoreStats.getMax());
        stats.put("minScore", scoreStats.getMin());
        stats.put("passRate", Math.round(passCount * 100.0 / grades.size()));
        stats.put("passCount", passCount);
        stats.put("failCount", grades.size() - passCount);
        return stats;
    }

    @Override
    public Grade getByAnswerSheet(Long answerSheetId) {
        return gradeMapper.selectOne(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getAnswerSheetId, answerSheetId));
    }
}
