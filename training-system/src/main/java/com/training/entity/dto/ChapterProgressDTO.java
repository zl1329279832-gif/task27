package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterProgressDTO {
    private Long chapterId;
    private String chapterTitle;
    private int sortOrder;
    private double progress;
    private int totalCoursewares;
    private int completedCoursewares;
    private int studyDurationSeconds;
}
