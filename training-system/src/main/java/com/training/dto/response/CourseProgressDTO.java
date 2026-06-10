package com.training.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CourseProgressDTO {

    private Long courseId;

    private String courseTitle;

    private Integer totalCoursewares;

    private Integer completedCoursewares;

    private BigDecimal completionRate;

    private List<ChapterProgressDTO> chapters;
}
