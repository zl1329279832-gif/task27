package com.training.service;

import com.training.entity.MakeupExamTask;

import java.time.LocalDateTime;
import java.util.List;

public interface MakeupExamTaskService {

    MakeupExamTask create(Long studentId, Long courseId, Long examId, Long pathId, String source);

    MakeupExamTask getById(Long id);

    List<MakeupExamTask> listByStudent(Long studentId, String status);

    void schedule(Long id, LocalDateTime scheduledAt);

    void complete(Long id, Long answerSheetId);

    void cancel(Long id);
}
