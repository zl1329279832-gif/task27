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
@TableName("certificate_renewal")
public class CertificateRenewal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long certificateId;

    private Long studentId;

    private Long courseId;

    private Long renewalRuleId;

    private String renewalAction;

    private String status;

    private Long newCertificateId;

    private Long learningPathId;

    private Long makeupExamId;

    private String rejectionReason;

    private Long processedBy;

    private LocalDateTime processedAt;

    private LocalDateTime deadline;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
