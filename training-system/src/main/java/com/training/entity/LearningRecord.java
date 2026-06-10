package com.training.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningRecord {

    private Long id;

    private Long studentId;

    private Long coursewareId;

    private Long chapterId;

    private Long courseId;

    private String status;

    private Integer progress;

    private Integer lastPosition;

    private Integer studyDuration;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime updatedAt;
}
