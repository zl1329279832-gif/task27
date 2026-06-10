package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = "answer_sheet", autoResultMap = true)
public class AnswerSheet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long studentId;

    private Integer attemptNo;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime submitTime;

    private Integer remainingSeconds;

    private Integer tabSwitchCount;

    private Double score;

    private Integer pass;

    private LocalDateTime gradingCompletedAt;

    /**
     * JSON snapshot of all questions at exam start time.
     * Ensures breakpoint resume and grading use frozen data even if the question bank changes.
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<QuestionSnapshot> questionSnapshot;

    private LocalDateTime createdAt;

    /**
     * Frozen question data captured at exam start.
     * Stored as JSON in the answer_sheet table to decouple from live question bank.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionSnapshot {
        private Long questionId;
        private Long examQuestionId;
        private String content;
        private String questionType;
        private List<String> options;
        private String correctAnswer;
        private Integer score;
        private Integer sortOrder;
    }
}
