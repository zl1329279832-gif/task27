package com.training.service;

import com.training.entity.KnowledgePointMastery;

import java.util.List;

public interface KnowledgePointMasteryService {

    void updateMasteryFromExam(Long studentId, Long courseId, Long answerSheetId);

    void recalculateMastery(Long studentId, Long courseId);

    List<KnowledgePointMastery> getUnmasteredPoints(Long studentId, Long courseId, double threshold);

    KnowledgePointMastery getMastery(Long studentId, Long knowledgePointId);
}
