package com.training.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HeartbeatRequest {
    @NotNull(message = "课件ID不能为空")
    private Long coursewareId;
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;
    private Integer currentPositionSeconds;
    private Integer studyDurationSeconds;
}
