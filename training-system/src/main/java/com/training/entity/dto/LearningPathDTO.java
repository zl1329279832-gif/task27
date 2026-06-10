package com.training.entity.dto;

import com.training.entity.LearningPath;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LearningPathDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String courseName;
    private String status;
    private String triggerReason;
    private Integer totalSteps;
    private Integer completedSteps;
    private List<LearningPath.PathStep> steps;
    private LocalDateTime generatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}
