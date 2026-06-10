package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class CourseRequest {

    @NotBlank
    private String title;

    private String description;

    private String coverImage;

    private String category;

    private String difficulty;

    private Integer totalHours;

    private BigDecimal completionThreshold;
}
