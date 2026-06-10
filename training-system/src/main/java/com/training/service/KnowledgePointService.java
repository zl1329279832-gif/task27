package com.training.service;

import com.training.entity.KnowledgePoint;
import com.training.entity.dto.KnowledgePointMasteryDTO;
import com.training.entity.dto.KnowledgePointRequest;

import java.util.List;

public interface KnowledgePointService {

    KnowledgePoint create(KnowledgePointRequest req);

    KnowledgePoint update(Long id, KnowledgePointRequest req);

    void delete(Long id);

    List<KnowledgePoint> listByCourse(Long courseId);

    List<KnowledgePoint> listByChapter(Long chapterId);

    void linkQuestionToKnowledgePoints(Long questionId, List<Long> kpIds);

    List<KnowledgePointMasteryDTO> getStudentMastery(Long studentId, Long courseId);
}
