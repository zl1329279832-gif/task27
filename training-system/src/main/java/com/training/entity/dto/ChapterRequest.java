package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterRequest {
    @NotBlank(message = "章节标题不能为空")
    private String title;
    private Integer sortOrder;
    private String description;
}
