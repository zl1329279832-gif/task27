package com.training.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AnswerSubmitRequest {
    @NotNull(message = "答卷ID不能为空")
    private Long answerSheetId;
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long examQuestionId;
        private Long questionId;
        private String answer;
    }
}
