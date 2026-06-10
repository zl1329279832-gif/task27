package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateRenewalServiceImpl implements CertificateRenewalService {

    private final CertificateRenewalMapper certificateRenewalMapper;
    private final CertificateMapper certificateMapper;
    private final CertificateRenewalRuleMapper renewalRuleMapper;
    private final CourseMapper courseMapper;
    private final GradeMapper gradeMapper;
    private final SysUserMapper sysUserMapper;
    private final LearningPathService learningPathService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CertificateRenewal evaluateRenewal(Long certificateId, Long operatorId) {
        Certificate cert = certificateMapper.selectById(certificateId);
        if (cert == null) throw new BusinessException("证书不存在");

        // CRITICAL: Revoked certificates cannot be renewed
        if ("REVOKED".equals(cert.getStatus())) {
            throw new BusinessException("已撤销的证书不能续期");
        }

        // Check for existing pending renewal
        CertificateRenewal existingRenewal = certificateRenewalMapper.selectOne(
                new LambdaQueryWrapper<CertificateRenewal>()
                        .eq(CertificateRenewal::getCertificateId, certificateId)
                        .eq(CertificateRenewal::getStatus, "PENDING"));
        if (existingRenewal != null) {
            return existingRenewal;
        }

        // Load renewal rule
        CertificateRenewalRule rule = renewalRuleMapper.selectOne(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getCourseId, cert.getCourseId())
                        .eq(CertificateRenewalRule::getEnabled, 1));
        if (rule == null) {
            throw new BusinessException("该课程未配置续期规则");
        }

        Course course = courseMapper.selectById(cert.getCourseId());
        if (course == null) throw new BusinessException("课程不存在");

        SysUser student = sysUserMapper.selectById(cert.getStudentId());

        String decision;
        String decisionReason;
        Double latestExamScore = null;

        // 1. Check role exemption
        if (student != null && rule.getRoleExemptions() != null
                && rule.getRoleExemptions().contains(student.getRole())) {
            decision = "DIRECT_RENEW";
            decisionReason = "角色豁免续期: " + student.getRole();
        }
        // 2. Check course version change
        else if (cert.getCourseVersion() != null && course.getVersion() != null
                && !cert.getCourseVersion().equals(course.getVersion())) {
            decision = rule.getVersionChangePolicy();
            decisionReason = "课程版本变更: v" + cert.getCourseVersion() + " -> v" + course.getVersion()
                    + ", 策略: " + rule.getVersionChangePolicy();
        }
        // 3. Check recent exam performance
        else {
            // Find the latest grade for this student+course
            List<Grade> grades = gradeMapper.selectList(
                    new LambdaQueryWrapper<Grade>()
                            .eq(Grade::getStudentId, cert.getStudentId())
                            .eq(Grade::getCourseId, cert.getCourseId())
                            .orderByDesc(Grade::getGradedAt));

            if (grades.isEmpty()) {
                decision = "RELEARN";
                decisionReason = "无考试记录，需要重新学习";
            } else {
                Grade latestGrade = grades.get(0);
                latestExamScore = latestGrade.getScore();

                if (latestGrade.getPass() == 1
                        && (rule.getMinExamScore() == null || latestGrade.getScore() >= rule.getMinExamScore())) {
                    decision = "DIRECT_RENEW";
                    decisionReason = "最近考试成绩达标: " + latestGrade.getScore();
                } else if (latestGrade.getPass() == 1) {
                    decision = "MAKEUP_EXAM";
                    decisionReason = "考试通过但成绩未达续期标准: " + latestGrade.getScore()
                            + " < " + rule.getMinExamScore();
                } else {
                    decision = "RELEARN";
                    decisionReason = "最近考试未通过: " + latestGrade.getScore();
                }
            }
        }

        // Create renewal record
        CertificateRenewal renewal = CertificateRenewal.builder()
                .certificateId(certificateId)
                .studentId(cert.getStudentId())
                .courseId(cert.getCourseId())
                .decision(decision)
                .decisionReason(decisionReason)
                .oldExpiryDate(cert.getExpiryDate())
                .courseVersionAtIssue(cert.getCourseVersion())
                .courseVersionCurrent(course.getVersion())
                .latestExamScore(latestExamScore)
                .operatorId(operatorId)
                .status("PENDING")
                .build();

        // Execute decision
        if ("DIRECT_RENEW".equals(decision)) {
            int renewalDays = rule.getRenewalPeriodDays();
            LocalDate newExpiry = LocalDate.now().plusDays(renewalDays);
            renewal.setNewExpiryDate(newExpiry);
            renewal.setStatus("COMPLETED");

            cert.setExpiryDate(newExpiry);
            cert.setCourseVersion(course.getVersion());
            cert.setStatus("VALID");
            certificateMapper.updateById(cert);
        } else if ("MAKEUP_EXAM".equals(decision) || "RELEARN".equals(decision)) {
            renewal.setStatus("PENDING");
        }

        certificateRenewalMapper.insert(renewal);

        // Generate learning path for MAKEUP_EXAM or RELEARN
        if ("MAKEUP_EXAM".equals(decision) || "RELEARN".equals(decision)) {
            try {
                learningPathService.generatePath(cert.getStudentId(), cert.getCourseId(), "RENEWAL");
            } catch (Exception e) {
                log.warn("Failed to generate learning path for renewal: {}", e.getMessage());
            }
        }

        auditLogService.log(operatorId, null, "RENEWAL_EVALUATED", "CERTIFICATE_RENEWAL", renewal.getId(),
                "{\"certificateId\":" + certificateId + ",\"decision\":\"" + decision
                        + "\",\"reason\":\"" + decisionReason + "\"}");

        return renewal;
    }

    @Override
    @Transactional
    public void executeRenewal(Long renewalId, Long operatorId) {
        CertificateRenewal renewal = certificateRenewalMapper.selectById(renewalId);
        if (renewal == null) throw new BusinessException("续期记录不存在");
        if ("COMPLETED".equals(renewal.getStatus())) throw new BusinessException("续期已完成");
        if ("REJECTED".equals(renewal.getStatus())) throw new BusinessException("续期已被拒绝");

        Certificate cert = certificateMapper.selectById(renewal.getCertificateId());
        if (cert == null) throw new BusinessException("证书不存在");
        if ("REVOKED".equals(cert.getStatus())) throw new BusinessException("已撤销的证书不能续期");

        CertificateRenewalRule rule = renewalRuleMapper.selectOne(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getCourseId, cert.getCourseId()));
        if (rule == null) throw new BusinessException("续期规则不存在");

        Course course = courseMapper.selectById(cert.getCourseId());

        int renewalDays = rule.getRenewalPeriodDays();
        LocalDate newExpiry = LocalDate.now().plusDays(renewalDays);

        cert.setExpiryDate(newExpiry);
        cert.setCourseVersion(course != null ? course.getVersion() : cert.getCourseVersion());
        cert.setStatus("VALID");
        certificateMapper.updateById(cert);

        renewal.setNewExpiryDate(newExpiry);
        renewal.setStatus("COMPLETED");
        certificateRenewalMapper.updateById(renewal);

        auditLogService.log(operatorId, null, "CERT_RENEWED", "CERTIFICATE", cert.getId(),
                "{\"renewalId\":" + renewalId + ",\"newExpiryDate\":\"" + newExpiry + "\"}");
    }

    @Override
    public List<CertificateRenewal> getHistory(Long certificateId) {
        return certificateRenewalMapper.selectList(
                new LambdaQueryWrapper<CertificateRenewal>()
                        .eq(CertificateRenewal::getCertificateId, certificateId)
                        .orderByDesc(CertificateRenewal::getCreatedAt));
    }

    @Override
    @Transactional
    public void scanExpiringCertificates() {
        // Find all renewal rules
        List<CertificateRenewalRule> rules = renewalRuleMapper.selectList(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getEnabled, 1));

        for (CertificateRenewalRule rule : rules) {
            LocalDate threshold = LocalDate.now().plusDays(rule.getAdvanceNoticeDays());

            List<Certificate> expiringCerts = certificateMapper.selectList(
                    new LambdaQueryWrapper<Certificate>()
                            .eq(Certificate::getCourseId, rule.getCourseId())
                            .eq(Certificate::getStatus, "VALID")
                            .isNotNull(Certificate::getExpiryDate)
                            .le(Certificate::getExpiryDate, threshold));

            for (Certificate cert : expiringCerts) {
                // Skip if pending renewal already exists
                Long pendingCount = certificateRenewalMapper.selectCount(
                        new LambdaQueryWrapper<CertificateRenewal>()
                                .eq(CertificateRenewal::getCertificateId, cert.getId())
                                .eq(CertificateRenewal::getStatus, "PENDING"));
                if (pendingCount > 0) continue;

                try {
                    evaluateRenewal(cert.getId(), null);
                } catch (Exception e) {
                    log.warn("Failed to evaluate renewal for certificate {}: {}", cert.getId(), e.getMessage());
                }
            }
        }
    }
}
