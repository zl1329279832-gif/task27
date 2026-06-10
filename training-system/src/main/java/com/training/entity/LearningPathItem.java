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
@TableName("learning_path_item")
public class LearningPathItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pathId;
    private String itemType;
    private Long refId;
    private Integer sortOrder;
    private String status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
