package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_mastery")
public class KnowledgeMastery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long chapterId;
    private String masteryLevel;
    private Double learningProgress;
    private Integer studyDurationSeconds;
    private Integer expectedDurationSeconds;
    private Integer wrongAnswerCount;
    private Integer totalAnswerCount;
    private Integer tabSwitchCount;
    private LocalDateTime lastEvaluatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
