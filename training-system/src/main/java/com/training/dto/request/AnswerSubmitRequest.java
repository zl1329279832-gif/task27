package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class AnswerSubmitRequest {

    @NotNull
    private Long submissionId;

    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        private Long questionId;

        private String answer;
    }
}
