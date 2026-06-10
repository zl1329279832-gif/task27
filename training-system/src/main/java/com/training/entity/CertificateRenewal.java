package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("certificate_renewal")
public class CertificateRenewal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long certificateId;
    private Long studentId;
    private Long courseId;
    private String decision;
    private String decisionReason;
    private LocalDate oldExpiryDate;
    private LocalDate newExpiryDate;
    private Integer courseVersionAtIssue;
    private Integer courseVersionCurrent;
    private Double latestExamScore;
    private Long operatorId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
