package com.training.service;

import com.training.entity.dto.ChapterProgressDTO;
import com.training.entity.dto.CourseProgressDTO;
import com.training.entity.dto.HeartbeatRequest;

import java.util.List;

public interface LearningRecordService {

    void heartbeat(Long studentId, HeartbeatRequest req);

    CourseProgressDTO getCourseProgress(Long studentId, Long courseId);

    List<ChapterProgressDTO> getChapterProgress(Long studentId, Long courseId);

    double getCompletionRate(Long studentId, Long courseId);
}
