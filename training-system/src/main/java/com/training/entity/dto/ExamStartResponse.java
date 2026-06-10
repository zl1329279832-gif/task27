package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExamStartResponse {
    private Long answerSheetId;
    private Integer attemptNo;
    private Integer remainingSeconds;
    private Integer totalQuestions;
    private Boolean resumed;
    private List<QuestionItem> questions;
    private List<AnswerItem> existingAnswers;

    @Data
    @Builder
    public static class QuestionItem {
        private Long examQuestionId;
        private Long questionId;
        private String content;
        private String questionType;
        private List<String> options;
        private Integer score;
        private Integer sortOrder;
    }

    @Data
    @Builder
    public static class AnswerItem {
        private Long examQuestionId;
        private Long questionId;
        private String studentAnswer;
    }
}
