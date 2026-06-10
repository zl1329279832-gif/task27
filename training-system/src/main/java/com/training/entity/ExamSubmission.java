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
public class ExamSubmission {

    private Long id;

    private Long examId;

    private Long studentId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime submitTime;

    private String status;

    private BigDecimal totalScore;

    private Integer tabSwitchCount;

    private Integer attemptNumber;

    private LocalDateTime createdAt;
}
