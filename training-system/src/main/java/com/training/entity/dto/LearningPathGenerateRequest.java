package com.training.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningPathGenerateRequest {

    @NotNull(message = "学员ID不能为空")
    private Long studentId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private String triggerReason;

    private Long clazzId;
}
