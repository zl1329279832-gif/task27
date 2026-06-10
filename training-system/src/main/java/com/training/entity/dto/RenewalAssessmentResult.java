package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RenewalAssessmentResult {
    private String renewalAction;
    private String reason;
    private Long matchedRuleId;
    private String matchedRuleDescription;
    private Long certificateId;
    private Long studentId;
    private Long courseId;
}
