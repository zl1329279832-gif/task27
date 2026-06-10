package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.CertificateRenewalRuleRequest;
import com.training.service.CertificateRenewalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate-renewal-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CertificateRenewalRuleController {

    private final CertificateRenewalService certificateRenewalService;

    @PostMapping
    public Result<?> create(@Valid @RequestBody CertificateRenewalRuleRequest req) {
        return Result.success(certificateRenewalService.createRule(req));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody CertificateRenewalRuleRequest req) {
        return Result.success(certificateRenewalService.updateRule(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        certificateRenewalService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> listByCourse(@PathVariable Long courseId) {
        return Result.success(certificateRenewalService.listRules(courseId));
    }
}
