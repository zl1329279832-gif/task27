package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRevokeRequest;
import com.training.mapper.*;
import com.training.service.CertificateService;
import com.training.service.LearningRecordService;
import com.training.util.CertNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateMapper certificateMapper;
    private final CertificateRevocationMapper revocationMapper;
    private final LearningRecordService learningRecordService;
    private final GradeMapper gradeMapper;
    private final ExamMapper examMapper;
    private final CertNoGenerator certNoGenerator;

    @Value("${certificate.verify-token-expiration-days:7}")
    private int verifyTokenExpirationDays;

    @Override
    @Transactional
    public Certificate issue(CertificateIssueRequest req, Long operatorId) {
        Long studentId = req.getStudentId();
        Long courseId = req.getCourseId();

        // Check if certificate already exists for this student+course
        Certificate existing = certificateMapper.selectOne(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getStudentId, studentId)
                        .eq(Certificate::getCourseId, courseId)
                        .eq(Certificate::getStatus, "VALID"));
        if (existing != null) {
            throw new BusinessException("该学员已获得此课程证书");
        }

        // Check course completion rate (100%)
        double completionRate = learningRecordService.getCompletionRate(studentId, courseId);
        if (completionRate < 100.0) {
            throw new BusinessException(String.format(
                    "课程完成率不足，当前: %.1f%%，要求: 100%%", completionRate));
        }

        // Check exam pass - find at least one passing grade for this student+course
        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, studentId)
                        .eq(Grade::getCourseId, courseId)
                        .eq(Grade::getPass, 1));
        if (grades.isEmpty()) {
            throw new BusinessException("学员未通过此课程的考试");
        }

        // Generate cert number (Redis INCR ensures uniqueness)
        String certNo = certNoGenerator.generate(courseId);

        // Generate verify token
        String verifyToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusDays(verifyTokenExpirationDays);

        // Determine title
        String title = req.getTitle();
        if (title == null || title.isBlank()) {
            title = "课程结业证书";
        }

        // Parse expiry date
        LocalDate expiryDate = null;
        if (req.getExpiryDate() != null && !req.getExpiryDate().isBlank()) {
            expiryDate = LocalDate.parse(req.getExpiryDate());
        }

        Certificate certificate = Certificate.builder()
                .studentId(studentId)
                .courseId(courseId)
                .certNo(certNo)
                .title(title)
                .issueDate(LocalDate.now())
                .expiryDate(expiryDate)
                .status("VALID")
                .verifyToken(verifyToken)
                .verifyTokenExpiresAt(tokenExpiresAt)
                .build();
        certificateMapper.insert(certificate);

        return certificate;
    }

    @Override
    public IPage<Certificate> listByStudent(Long studentId, int page, int size) {
        return certificateMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getStudentId, studentId)
                        .orderByDesc(Certificate::getCreatedAt));
    }

    @Override
    public IPage<Certificate> listAll(int page, int size, String status) {
        LambdaQueryWrapper<Certificate> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Certificate::getStatus, status);
        wrapper.orderByDesc(Certificate::getCreatedAt);
        return certificateMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Certificate getById(Long id) {
        Certificate cert = certificateMapper.selectById(id);
        if (cert == null) throw new BusinessException("证书不存在");
        return cert;
    }

    @Override
    @Transactional
    public void revoke(Long id, CertificateRevokeRequest req, Long operatorId) {
        Certificate cert = certificateMapper.selectById(id);
        if (cert == null) throw new BusinessException("证书不存在");
        if ("REVOKED".equals(cert.getStatus())) throw new BusinessException("证书已被撤销");

        // Update certificate status
        cert.setStatus("REVOKED");
        cert.setRevokeReason(req.getReason());
        cert.setRevokedBy(operatorId);
        cert.setRevokedAt(LocalDateTime.now());
        certificateMapper.updateById(cert);

        // Create revocation record
        CertificateRevocation revocation = CertificateRevocation.builder()
                .certificateId(id)
                .reason(req.getReason())
                .revokedBy(operatorId)
                .build();
        revocationMapper.insert(revocation);
    }

    @Override
    public Map<String, Object> verifyByCertNo(String certNo) {
        Map<String, Object> result = new HashMap<>();

        Certificate cert = certificateMapper.selectOne(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getCertNo, certNo));

        if (cert == null) {
            result.put("valid", false);
            result.put("message", "证书编号不存在");
            return result;
        }

        return buildVerifyResult(cert);
    }

    @Override
    public Map<String, Object> verifyByToken(String token) {
        Map<String, Object> result = new HashMap<>();

        Certificate cert = certificateMapper.selectOne(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getVerifyToken, token));

        if (cert == null) {
            result.put("valid", false);
            result.put("message", "验证链接无效");
            return result;
        }

        // Check token expiration
        if (cert.getVerifyTokenExpiresAt() != null
                && LocalDateTime.now().isAfter(cert.getVerifyTokenExpiresAt())) {
            result.put("valid", false);
            result.put("message", "验证链接已过期，请联系管理员重新生成");
            result.put("expired", true);
            result.put("certId", cert.getId());
            return result;
        }

        return buildVerifyResult(cert);
    }

    private Map<String, Object> buildVerifyResult(Certificate cert) {
        Map<String, Object> result = new HashMap<>();

        if ("REVOKED".equals(cert.getStatus())) {
            result.put("valid", false);
            result.put("message", "该证书已撤销");
            result.put("revokeReason", cert.getRevokeReason());
            result.put("revokedAt", cert.getRevokedAt());
            return result;
        }

        if ("EXPIRED".equals(cert.getStatus())) {
            result.put("valid", false);
            result.put("message", "该证书已过期");
            result.put("expiryDate", cert.getExpiryDate());
            return result;
        }

        // Check if expiry date has passed
        if (cert.getExpiryDate() != null && LocalDate.now().isAfter(cert.getExpiryDate())) {
            cert.setStatus("EXPIRED");
            certificateMapper.updateById(cert);
            result.put("valid", false);
            result.put("message", "该证书已过期");
            result.put("expiryDate", cert.getExpiryDate());
            return result;
        }

        result.put("valid", true);
        result.put("message", "证书有效");
        result.put("certNo", cert.getCertNo());
        result.put("title", cert.getTitle());
        result.put("issueDate", cert.getIssueDate());
        result.put("expiryDate", cert.getExpiryDate());
        result.put("studentId", cert.getStudentId());
        result.put("courseId", cert.getCourseId());
        return result;
    }

    @Override
    public String regenerateVerifyToken(Long id) {
        Certificate cert = certificateMapper.selectById(id);
        if (cert == null) throw new BusinessException("证书不存在");

        String newToken = UUID.randomUUID().toString().replace("-", "");
        cert.setVerifyToken(newToken);
        cert.setVerifyTokenExpiresAt(LocalDateTime.now().plusDays(verifyTokenExpirationDays));
        certificateMapper.updateById(cert);

        return newToken;
    }
}
