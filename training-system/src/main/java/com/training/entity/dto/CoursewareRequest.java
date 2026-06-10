package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CoursewareRequest {
    @NotBlank(message = "课件标题不能为空")
    private String title;
    private String fileType;
    private Integer durationSeconds;
    private Integer sortOrder;
    private String linkUrl;
}
