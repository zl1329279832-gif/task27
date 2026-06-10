package com.training.controller;

import com.training.common.Result;
import com.training.entity.CertificateRenewal;
import com.training.entity.CertificateRenewalRule;
import com.training.entity.dto.RenewalRuleRequest;
import com.training.security.CustomUserDetails;
import com.training.service.CertificateRenewalRuleService;
import com.training.service.CertificateRenewalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificate-renewals")
@RequiredArgsConstructor
public class CertificateRenewalController {

    private final CertificateRenewalRuleService renewalRuleService;
    private final CertificateRenewalService renewalService;

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CertificateRenewalRule> createRule(@Valid @RequestBody RenewalRuleRequest req) {
        return Result.success(renewalRuleService.create(req));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CertificateRenewalRule> updateRule(@PathVariable Long id,
                                                      @RequestBody RenewalRuleRequest req) {
        return Result.success(renewalRuleService.update(id, req));
    }

    @GetMapping("/rules/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<CertificateRenewalRule> getRuleByCourse(@PathVariable Long courseId) {
        return Result.success(renewalRuleService.getByCourse(courseId));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteRule(@PathVariable Long id) {
        renewalRuleService.delete(id);
        return Result.success();
    }

    @PostMapping("/evaluate/{certificateId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<CertificateRenewal> evaluate(@PathVariable Long certificateId) {
        Long operatorId = getCurrentUserId();
        return Result.success(renewalService.evaluateRenewal(certificateId, operatorId));
    }

    @PostMapping("/{renewalId}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> execute(@PathVariable Long renewalId) {
        Long operatorId = getCurrentUserId();
        renewalService.executeRenewal(renewalId, operatorId);
        return Result.success();
    }

    @GetMapping("/certificate/{certificateId}/history")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<List<CertificateRenewal>> getHistory(@PathVariable Long certificateId) {
        return Result.success(renewalService.getHistory(certificateId));
    }

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUserId();
    }
}
