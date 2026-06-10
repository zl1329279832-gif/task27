package com.training.service;

import com.training.entity.KnowledgeMastery;
import com.training.entity.dto.KnowledgeMasteryDTO;

import java.util.List;

public interface KnowledgeMasteryService {

    List<KnowledgeMastery> evaluateMastery(Long studentId, Long courseId);

    KnowledgeMastery getChapterMastery(Long studentId, Long chapterId);

    List<KnowledgeMasteryDTO> getMasteryByCourse(Long studentId, Long courseId);
}
