package com.training.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "course_version_history", autoResultMap = true)
public class CourseVersionHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Integer fromVersion;

    private Integer toVersion;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object changeSummary;

    private Long changedBy;

    private LocalDateTime changedAt;
}
