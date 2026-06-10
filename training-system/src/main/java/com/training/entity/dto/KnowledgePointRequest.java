package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgePointRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Long chapterId;

    @NotBlank(message = "知识点名称不能为空")
    private String name;

    private String description;

    private Integer sortOrder;

    private Long parentKpId;

    private List<Long> questionIds;
}
