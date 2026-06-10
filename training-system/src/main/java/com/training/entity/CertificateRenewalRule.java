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
@TableName("certificate_renewal_rule")
public class CertificateRenewalRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String rolePattern;

    private Integer expiryThresholdDays;

    private Integer minCourseVersion;

    private BigDecimal minRecentExamScore;

    private Integer recentExamWithinMonths;

    private String renewalAction;

    private String description;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
