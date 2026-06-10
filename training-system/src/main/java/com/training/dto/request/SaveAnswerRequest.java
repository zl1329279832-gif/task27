package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class SaveAnswerRequest {

    @NotNull
    private Long submissionId;

    @NotNull
    private Long questionId;

    private String answer;
}
