package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.LearningPath;
import com.training.entity.dto.LearningPathDTO;

public interface LearningPathService {

    LearningPath generatePath(Long studentId, Long courseId, String reason);

    LearningPathDTO getActivePath(Long studentId, Long courseId);

    IPage<LearningPath> listByStudent(Long studentId, int page, int size);

    void updateItemStatus(Long pathItemId, String status);

    void checkPathCompletion(Long pathId);
}
