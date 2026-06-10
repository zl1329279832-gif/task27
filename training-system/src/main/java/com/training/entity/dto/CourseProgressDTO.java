package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseProgressDTO {
    private Long courseId;
    private String courseTitle;
    private double overallProgress;
    private int totalChapters;
    private int completedChapters;
    private int totalCoursewares;
    private int completedCoursewares;
    private int totalStudyDurationSeconds;
}
