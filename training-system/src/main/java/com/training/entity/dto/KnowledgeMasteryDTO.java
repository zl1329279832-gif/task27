package com.training.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeMasteryDTO {
    private Long chapterId;
    private String chapterTitle;
    private String masteryLevel;
    private Double learningProgress;
    private Integer wrongAnswerCount;
    private Integer totalAnswerCount;
    private Integer studyDurationSeconds;
    private Integer expectedDurationSeconds;
}
