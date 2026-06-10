package com.training.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RenewalRuleRequest {
    @NotNull
    private Long courseId;
    @NotNull
    private Integer renewalPeriodDays;
    private Integer advanceNoticeDays;
    private Boolean requireExamPass;
    private Integer minExamScore;
    private String versionChangePolicy;
    private String roleExemptions;
    private Boolean enabled;
}
