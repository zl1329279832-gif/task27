package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class QuestionRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String content;

    @NotBlank
    private String questionType;

    private String options;

    @NotBlank
    private String correctAnswer;

    private String analysis;

    private BigDecimal score;

    private String difficulty;
}
