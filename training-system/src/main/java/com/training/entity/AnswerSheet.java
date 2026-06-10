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
@TableName("answer_sheet")
public class AnswerSheet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long studentId;

    private Integer attemptNo;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime submitTime;

    private Integer remainingSeconds;

    private Integer tabSwitchCount;

    private Double score;

    private Integer pass;

    private LocalDateTime gradingCompletedAt;

    private LocalDateTime createdAt;
}
