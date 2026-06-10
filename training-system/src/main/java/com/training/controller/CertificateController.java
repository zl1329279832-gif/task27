package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.CertificateIssueRequest;
import com.training.dto.request.CertificateRevokeRequest;
import com.training.dto.response.PageResult;
import com.training.dto.response.Result;
import com.training.entity.Certificate;
import com.training.security.CustomUserDetails;
import com.training.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@Slf4j
public class CertificateController {

    private final CertificateService certificateService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<Certificate> issue(@RequestBody @Valid CertificateIssueRequest request) {
        Long issuedBy = getCurrentUserId();
        log.info("颁发证书: studentId={}, issuedBy={}", request.getStudentId(), issuedBy);
        Certificate certificate = certificateService.issueCertificate(request, issuedBy);
        return Result.success(certificate);
    }

    @GetMapping("/{id}")
    public Result<Certificate> getById(@PathVariable Long id) {
        log.info("查询证书详情: id={}", id);
        Certificate certificate = certificateService.getCertificateById(id);
        return Result.success(certificate);
    }

    @GetMapping("/student/{studentId}")
    public Result<List<Certificate>> getByStudent(@PathVariable Long studentId) {
        log.info("查询学生证书: studentId={}", studentId);
        List<Certificate> certificates = certificateService.getCertificatesByStudent(studentId);
        return Result.success(certificates);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<PageResult<Certificate>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询证书列表: keyword={}, status={}, page={}, size={}", keyword, status, page, size);
        PageResult<Certificate> result = certificateService.listCertificates(keyword, status, page, size);
        return Result.success(result);
    }

    @GetMapping("/verify/cert-no/{no}")
    public Result<Map<String, Object>> verifyByCertNo(@PathVariable String no) {
        log.info("通过证书编号验证: certNo={}", no);
        Map<String, Object> result = certificateService.verifyByCertNo(no);
        return Result.success(result);
    }

    @GetMapping("/verify/token/{token}")
    public Result<Map<String, Object>> verifyByToken(@PathVariable String token) {
        log.info("通过Token验证证书: token={}", token);
        Map<String, Object> result = certificateService.verifyByToken(token);
        return Result.success(result);
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<Void> revoke(@RequestBody @Valid CertificateRevokeRequest request) {
        Long revokedBy = getCurrentUserId();
        log.info("撤销证书: certificateId={}, revokedBy={}", request.getCertificateId(), revokedBy);
        certificateService.revokeCertificate(request, revokedBy);
        return Result.success();
    }

    @PostMapping("/{id}/regenerate-token")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<String> regenerateToken(@PathVariable Long id) {
        log.info("重新生成证书Token: id={}", id);
        String token = certificateService.regenerateVerifyToken(id);
        return Result.success(token);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Certificate>> getMyCertificates() {
        Long studentId = getCurrentUserId();
        log.info("查询我的证书: studentId={}", studentId);
        List<Certificate> certificates = certificateService.getCertificatesByStudent(studentId);
        return Result.success(certificates);
    }
}
