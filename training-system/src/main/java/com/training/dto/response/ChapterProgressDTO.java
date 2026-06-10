package com.training.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChapterProgressDTO {

    private Long chapterId;

    private String chapterTitle;

    private Integer totalCoursewares;

    private Integer completedCoursewares;

    private BigDecimal completionRate;
}
