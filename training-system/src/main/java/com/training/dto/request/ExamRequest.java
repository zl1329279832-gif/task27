package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ExamRequest {

    @NotBlank
    private String title;

    @NotNull
    private Long courseId;

    @NotNull
    private Integer durationMinutes;

    private BigDecimal totalScore;

    @NotNull
    private BigDecimal passScore;

    @NotNull
    private Integer questionCount;

    private Integer maxAttempts;
}
