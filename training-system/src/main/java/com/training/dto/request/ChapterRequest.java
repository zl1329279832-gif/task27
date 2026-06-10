package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ChapterRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String title;

    private String description;

    private Integer sortOrder;
}
