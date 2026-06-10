package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.LearningPath;
import com.training.entity.dto.LearningPathDTO;

public interface LearningPathService {

    LearningPath generatePath(Long studentId, Long courseId, String triggerReason, Long operatorId);

    LearningPathDTO getActivePath(Long studentId, Long courseId);

    void completePathStep(Long pathId, Integer stepOrder);

    IPage<LearningPath> listPaths(Long studentId, int page, int size);

    void expirePath(Long pathId);
}
