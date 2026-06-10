package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("certificate")
public class Certificate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Integer courseVersion;

    @TableField("cert_no")
    private String certNo;

    private String title;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private String status;

    private String revokeReason;

    private Long revokedBy;

    private LocalDateTime revokedAt;

    private String verifyToken;

    private LocalDateTime verifyTokenExpiresAt;

    private String filePath;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
