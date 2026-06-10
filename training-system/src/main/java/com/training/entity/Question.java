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
public class Question {

    private Long id;

    private Long courseId;

    private String content;

    private String questionType;

    private String options;

    private String correctAnswer;

    private String analysis;

    private BigDecimal score;

    private String difficulty;

    private String status;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
