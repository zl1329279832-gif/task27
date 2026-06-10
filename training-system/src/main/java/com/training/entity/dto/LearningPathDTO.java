package com.training.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String title;
    private String status;
    private Integer totalItems;
    private Integer completedItems;
    private String generatedReason;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private List<LearningPathItemDTO> items;
}
