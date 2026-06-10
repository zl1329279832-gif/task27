package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class CertificateIssueRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long courseId;

    private Long examId;

    private String title;

    private String description;
}
