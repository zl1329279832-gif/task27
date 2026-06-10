package com.training.service;

import com.training.dto.response.CourseProgressDTO;
import com.training.entity.LearningRecord;

import java.math.BigDecimal;
import java.util.List;

public interface LearningRecordService {

    LearningRecord updateProgress(Long studentId, Long coursewareId, Integer progress, Integer lastPosition, Integer studyDuration);

    List<LearningRecord> getRecordsByCourse(Long studentId, Long courseId);

    CourseProgressDTO getCourseProgress(Long studentId, Long courseId);

    BigDecimal getCourseCompletionRate(Long studentId, Long courseId);
}
