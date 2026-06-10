package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamRequest {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    @NotBlank(message = "考试标题不能为空")
    private String title;
    private String description;
    @NotNull(message = "考试时长不能为空")
    private Integer durationMinutes;
    private Integer totalScore;
    private Integer passScore;
    private Integer questionCount;
    private Boolean randomize;
    private Integer maxAttempts;
    private Boolean antiCheatEnabled;
    private Integer maxTabSwitches;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ExamQuestionItem> questions;

    @Data
    public static class ExamQuestionItem {
        @NotNull
        private Long questionId;
        private Integer sortOrder;
        private Integer scoreOverride;
    }
}
