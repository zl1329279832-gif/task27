package com.training.service;

import com.training.entity.RemedialTask;

import java.util.List;

public interface RemedialTaskService {

    RemedialTask create(Long studentId, Long courseId, Long chapterId, Long pathId, String source);

    RemedialTask getById(Long id);

    List<RemedialTask> listByStudent(Long studentId, String status);

    void start(Long id);

    void complete(Long id);

    void cancel(Long id);
}
