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
@TableName(value = "audit_log", autoResultMap = true)
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String action;

    private String targetType;

    private Long targetId;

    private Long actorId;

    private String actorRole;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object details;

    private String ipAddress;

    private LocalDateTime createdAt;
}
