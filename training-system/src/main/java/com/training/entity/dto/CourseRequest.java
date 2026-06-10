package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseRequest {
    @NotBlank(message = "课程标题不能为空")
    private String title;
    private String description;
    private String coverImage;
    private String category;
    private String difficulty;
    private Double totalHours;
}
