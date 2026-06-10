package com.training.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    private Long id;

    private String title;

    private String description;

    private String coverImage;

    private Long instructorId;

    private String category;

    private String difficulty;

    private Integer totalHours;

    private String status;

    private BigDecimal completionThreshold;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
