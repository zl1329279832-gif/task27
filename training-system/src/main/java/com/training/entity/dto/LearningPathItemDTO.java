package com.training.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathItemDTO {
    private Long id;
    private Long pathId;
    private String itemType;
    private Long refId;
    private Integer sortOrder;
    private String status;
    private LocalDateTime completedAt;
    private String chapterTitle;
    private String examTitle;
}
