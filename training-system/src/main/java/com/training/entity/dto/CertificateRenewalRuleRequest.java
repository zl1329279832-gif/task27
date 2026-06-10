package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CertificateRenewalRuleRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private String rolePattern;

    private Integer expiryThresholdDays;

    private Integer minCourseVersion;

    private BigDecimal minRecentExamScore;

    private Integer recentExamWithinMonths;

    @NotBlank(message = "续期动作不能为空")
    private String renewalAction;

    private String description;

    private Integer sortOrder;

    private Integer enabled;
}
