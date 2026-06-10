package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MakeupExamDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String courseName;
    private Long examId;
    private String examTitle;
    private Long learningPathId;
    private Long answerSheetId;
    private String status;
    private Integer maxAttempts;
    private Integer attemptsUsed;
    private BigDecimal requiredScore;
    private BigDecimal achievedScore;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
