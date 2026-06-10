package com.training.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ExamStartResponse {

    private Long submissionId;

    private Long examId;

    private Integer durationMinutes;

    private Long remainingSeconds;

    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {

        private Long questionId;

        private String content;

        private String questionType;

        private String options;

        private BigDecimal score;

        private String studentAnswer;
    }
}
