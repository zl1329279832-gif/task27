package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    @NotBlank(message = "题目内容不能为空")
    private String content;
    @NotBlank(message = "题目类型不能为空")
    private String questionType;
    private List<String> options;
    @NotBlank(message = "正确答案不能为空")
    private String correctAnswer;
    private Integer score;
    private String difficulty;
    private String explanation;
}
