package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.AnswerSheet;
import com.training.entity.AnswerDetail;

import java.util.List;

public interface AnswerSheetService {
    IPage<AnswerSheet> list(int page, int size, Long examId, Long studentId);
    AnswerSheetDetailDTO getDetail(Long id, Long currentUserId, String currentRole);
    AnswerSheet regrade(Long id, Long graderId);

    class AnswerSheetDetailDTO {
        private AnswerSheet answerSheet;
        private List<AnswerDetailItem> details;

        public AnswerSheet getAnswerSheet() { return answerSheet; }
        public void setAnswerSheet(AnswerSheet answerSheet) { this.answerSheet = answerSheet; }
        public List<AnswerDetailItem> getDetails() { return details; }
        public void setDetails(List<AnswerDetailItem> details) { this.details = details; }

        public static class AnswerDetailItem {
            private Long detailId;
            private Long questionId;
            private String questionContent;
            private String questionType;
            private String correctAnswer;
            private String studentAnswer;
            private Integer isCorrect;
            private Double scoreEarned;
            private Integer maxScore;
            private String gradingNote;
            private Boolean questionDeleted;

            public Long getDetailId() { return detailId; }
            public void setDetailId(Long detailId) { this.detailId = detailId; }
            public Long getQuestionId() { return questionId; }
            public void setQuestionId(Long questionId) { this.questionId = questionId; }
            public String getQuestionContent() { return questionContent; }
            public void setQuestionContent(String v) { this.questionContent = v; }
            public String getQuestionType() { return questionType; }
            public void setQuestionType(String v) { this.questionType = v; }
            public String getCorrectAnswer() { return correctAnswer; }
            public void setCorrectAnswer(String v) { this.correctAnswer = v; }
            public String getStudentAnswer() { return studentAnswer; }
            public void setStudentAnswer(String v) { this.studentAnswer = v; }
            public Integer getIsCorrect() { return isCorrect; }
            public void setIsCorrect(Integer v) { this.isCorrect = v; }
            public Double getScoreEarned() { return scoreEarned; }
            public void setScoreEarned(Double v) { this.scoreEarned = v; }
            public Integer getMaxScore() { return maxScore; }
            public void setMaxScore(Integer v) { this.maxScore = v; }
            public String getGradingNote() { return gradingNote; }
            public void setGradingNote(String v) { this.gradingNote = v; }
            public Boolean getQuestionDeleted() { return questionDeleted; }
            public void setQuestionDeleted(Boolean v) { this.questionDeleted = v; }
        }
    }
}
