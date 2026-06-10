package com.training.service;

import com.training.entity.Score;

import java.math.BigDecimal;
import java.util.List;

public interface ScoreService {

    Score createScore(Long studentId, Long examId, Long submissionId, BigDecimal totalScore, BigDecimal passScore);

    List<Score> getScoresByStudent(Long studentId);

    List<Score> getScoresByExam(Long examId);

    Score getScoreById(Long id);

    void approveScore(Long id, Long reviewerId);

    void rejectScore(Long id, Long reviewerId);

    Score getBestScore(Long studentId, Long examId);
}
