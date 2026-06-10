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
public class Score {

    private Long id;

    private Long studentId;

    private Long examId;

    private Long submissionId;

    private BigDecimal totalScore;

    private BigDecimal passScore;

    private Integer isPassed;

    private String status;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
