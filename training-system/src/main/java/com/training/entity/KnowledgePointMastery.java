package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_point_mastery")
public class KnowledgePointMastery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long knowledgePointId;

    private Long courseId;

    private BigDecimal masteryLevel;

    private Integer totalQuestions;

    private Integer correctCount;

    private Integer examAttempts;

    private String status;

    private LocalDateTime lastAssessedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
