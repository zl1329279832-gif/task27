package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgePointMasteryDTO {
    private Long id;
    private Long studentId;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Long courseId;
    private BigDecimal masteryLevel;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer examAttempts;
    private String status;
    private LocalDateTime lastAssessedAt;
}
