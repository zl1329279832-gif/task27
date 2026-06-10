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
@TableName("courseware")
public class Courseware {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;

    private String title;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private Integer durationSeconds;

    private Integer sortOrder;

    private String status;

    private LocalDateTime createdAt;
}
