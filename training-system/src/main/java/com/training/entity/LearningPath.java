package com.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "learning_path", autoResultMap = true)
public class LearningPath {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Long clazzId;

    private String status;

    private String triggerReason;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<PathStep> pathData;

    private Integer totalSteps;

    private Integer completedSteps;

    private Long generatedBy;

    private LocalDateTime generatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathStep {
        private Integer stepOrder;
        private String stepType;
        private Long targetId;
        private String targetTitle;
        private String status;
        private LocalDateTime completedAt;
    }
}
