package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.RemedialTask;

public interface RemedialTaskService {

    RemedialTask create(RemedialTask task);

    IPage<RemedialTask> listByStudent(Long studentId, int page, int size);

    void markProgress(Long taskId, double achievedProgress);

    void complete(Long taskId);

    void expire(Long taskId);
}
