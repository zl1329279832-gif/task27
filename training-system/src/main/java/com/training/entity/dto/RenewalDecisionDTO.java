package com.training.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalDecisionDTO {
    private Long renewalId;
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
    private String status;
}
