package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClazzRequest {
    @NotBlank(message = "班级名称不能为空")
    private String name;
    private String description;
    private Long instructorId;
}
