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
public class SubmissionAnswer {

    private Long id;

    private Long submissionId;

    private Long questionId;

    private String questionSnapshot;

    private String studentAnswer;

    private Integer isCorrect;

    private BigDecimal awardedScore;

    private LocalDateTime answeredAt;
}
