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
public class Exam {

    private Long id;

    private String title;

    private Long courseId;

    private Integer durationMinutes;

    private BigDecimal totalScore;

    private BigDecimal passScore;

    private Integer questionCount;

    private Integer maxAttempts;

    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
