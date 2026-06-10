package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RemedialTaskDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String courseName;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Long chapterId;
    private String chapterTitle;
    private String taskType;
    private String status;
    private BigDecimal requiredProgress;
    private BigDecimal achievedProgress;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
