package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRenewalRuleRequest;
import com.training.entity.dto.RenewalAssessmentResult;
import com.training.mapper.*;
import com.training.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateRenewalServiceImpl implements CertificateRenewalService {

    private final CertificateRenewalMapper certificateRenewalMapper;
    private final CertificateRenewalRuleMapper certificateRenewalRuleMapper;
    private final CertificateMapper certificateMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper sysUserMapper;
    private final GradeMapper gradeMapper;
    private final ExamMapper examMapper;
    private final CertificateService certificateService;
    private final LearningPathService learningPathService;
    private final MakeupExamService makeupExamService;
    private final AuditLogService auditLogService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public RenewalAssessmentResult assessRenewal(Long certificateId) {
        Certificate cert = certificateMapper.selectById(certificateId);
        if (cert == null) throw new BusinessException("证书不存在");

        // Block: REVOKED certificate
        if ("REVOKED".equals(cert.getStatus())) {
            throw new BusinessException("证书已撤销，无法续期");
        }

        // Block: expired more than 1 year
        if ("EXPIRED".equals(cert.getStatus()) && cert.getExpiryDate() != null) {
            long monthsExpired = ChronoUnit.MONTHS.between(cert.getExpiryDate(), LocalDate.now());
            if (monthsExpired > 12) {
                throw new BusinessException("证书已过期超过1年，无法续期");
            }
        }

        // Load student role
        SysUser student = sysUserMapper.selectById(cert.getStudentId());
        String studentRole = student != null ? student.getRole() : "STUDENT";

        // Load current course version
        Course course = courseMapper.selectById(cert.getCourseId());
        int courseVersion = (course != null && course.getVersion() != null) ? course.getVersion() : 1;

        // Load recent exam score
        BigDecimal recentExamScore = getRecentExamScore(cert.getStudentId(), cert.getCourseId(), 12);

        // Load renewal rules
        List<CertificateRenewalRule> rules = certificateRenewalRuleMapper.selectList(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getCourseId, cert.getCourseId())
                        .eq(CertificateRenewalRule::getEnabled, 1)
                        .orderByAsc(CertificateRenewalRule::getSortOrder));

        // Match first rule
        for (CertificateRenewalRule rule : rules) {
            if (matchesRule(rule, studentRole, courseVersion, recentExamScore)) {
                return RenewalAssessmentResult.builder()
                        .renewalAction(rule.getRenewalAction())
                        .reason(rule.getDescription())
                        .matchedRuleId(rule.getId())
                        .matchedRuleDescription(rule.getDescription())
                        .certificateId(certificateId)
                        .studentId(cert.getStudentId())
                        .courseId(cert.getCourseId())
                        .build();
            }
        }

        // Default: FULL_RELEARN
        return RenewalAssessmentResult.builder()
                .renewalAction("FULL_RELEARN")
                .reason("未匹配任何续期规则，默认需要重新学习")
                .certificateId(certificateId)
                .studentId(cert.getStudentId())
                .courseId(cert.getCourseId())
                .build();
    }

    @Override
    @Transactional
    public CertificateRenewal initiateRenewal(Long certificateId, Long operatorId) {
        // Redis lock to prevent concurrent renewal
        String lockKey = "renewal:lock:" + certificateId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("续期操作正在进行中，请勿重复提交");
        }

        try {
            RenewalAssessmentResult assessment = assessRenewal(certificateId);
            Certificate cert = certificateMapper.selectById(certificateId);

            // Check if there's already an active renewal
            CertificateRenewal existingRenewal = certificateRenewalMapper.selectOne(
                    new LambdaQueryWrapper<CertificateRenewal>()
                            .eq(CertificateRenewal::getCertificateId, certificateId)
                            .in(CertificateRenewal::getStatus, "PENDING", "IN_PROGRESS"));
            if (existingRenewal != null) {
                throw new BusinessException("已存在进行中的续期记录");
            }

            CertificateRenewal renewal = CertificateRenewal.builder()
                    .certificateId(certificateId)
                    .studentId(cert.getStudentId())
                    .courseId(cert.getCourseId())
                    .renewalRuleId(assessment.getMatchedRuleId())
                    .renewalAction(assessment.getRenewalAction())
                    .build();

            switch (assessment.getRenewalAction()) {
                case "DIRECT_RENEWAL":
                    // Issue new certificate immediately
                    CertificateIssueRequest issueReq = new CertificateIssueRequest();
                    issueReq.setStudentId(cert.getStudentId());
                    issueReq.setCourseId(cert.getCourseId());
                    issueReq.setTitle(cert.getTitle());
                    Certificate newCert = certificateService.issue(issueReq, operatorId);
                    renewal.setNewCertificateId(newCert.getId());
                    renewal.setStatus("COMPLETED");
                    renewal.setProcessedBy(operatorId);
                    renewal.setProcessedAt(LocalDateTime.now());
                    break;

                case "MAKEUP_EXAM":
                    // Create makeup exam
                    MakeupExam makeup = MakeupExam.builder()
                            .studentId(cert.getStudentId())
                            .courseId(cert.getCourseId())
                            .examId(findExamForCourse(cert.getCourseId()))
                            .status("PENDING")
                            .maxAttempts(2)
                            .attemptsUsed(0)
                            .requiredScore(BigDecimal.valueOf(60))
                            .build();
                    MakeupExam createdMakeup = makeupExamService.create(makeup);
                    renewal.setMakeupExamId(createdMakeup.getId());
                    renewal.setStatus("IN_PROGRESS");
                    break;

                case "FULL_RELEARN":
                default:
                    // Generate learning path
                    LearningPath path = learningPathService.generatePath(
                            cert.getStudentId(), cert.getCourseId(), "CERT_RENEWAL", operatorId);
                    renewal.setLearningPathId(path.getId());
                    renewal.setStatus("IN_PROGRESS");
                    break;
            }

            certificateRenewalMapper.insert(renewal);

            auditLogService.log("RENEWAL_INITIATED", "CERTIFICATE_RENEWAL", renewal.getId(),
                    operatorId, null,
                    Map.of("certificateId", certificateId,
                            "action", assessment.getRenewalAction()));

            return renewal;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public void completeRenewal(Long renewalId, Long operatorId) {
        CertificateRenewal renewal = certificateRenewalMapper.selectById(renewalId);
        if (renewal == null) throw new BusinessException("续期记录不存在");
        if (!"IN_PROGRESS".equals(renewal.getStatus()))
            throw new BusinessException("续期记录状态不允许完成");

        // Issue new certificate
        Certificate oldCert = certificateMapper.selectById(renewal.getCertificateId());
        CertificateIssueRequest issueReq = new CertificateIssueRequest();
        issueReq.setStudentId(renewal.getStudentId());
        issueReq.setCourseId(renewal.getCourseId());
        issueReq.setTitle(oldCert != null ? oldCert.getTitle() : "续期证书");
        Certificate newCert = certificateService.issue(issueReq, operatorId);

        renewal.setNewCertificateId(newCert.getId());
        renewal.setStatus("COMPLETED");
        renewal.setProcessedBy(operatorId);
        renewal.setProcessedAt(LocalDateTime.now());
        certificateRenewalMapper.updateById(renewal);

        auditLogService.log("RENEWAL_COMPLETED", "CERTIFICATE_RENEWAL", renewalId,
                operatorId, null,
                Map.of("newCertificateId", newCert.getId()));
    }

    @Override
    @Transactional
    public void rejectRenewal(Long renewalId, String reason, Long operatorId) {
        CertificateRenewal renewal = certificateRenewalMapper.selectById(renewalId);
        if (renewal == null) throw new BusinessException("续期记录不存在");
        renewal.setStatus("REJECTED");
        renewal.setRejectionReason(reason);
        renewal.setProcessedBy(operatorId);
        renewal.setProcessedAt(LocalDateTime.now());
        certificateRenewalMapper.updateById(renewal);

        auditLogService.log("RENEWAL_REJECTED", "CERTIFICATE_RENEWAL", renewalId,
                operatorId, null, Map.of("reason", reason));
    }

    @Override
    public IPage<CertificateRenewal> listByStudent(Long studentId, int page, int size) {
        return certificateRenewalMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CertificateRenewal>()
                        .eq(CertificateRenewal::getStudentId, studentId)
                        .orderByDesc(CertificateRenewal::getCreatedAt));
    }

    @Override
    public IPage<CertificateRenewal> listAll(int page, int size) {
        return certificateRenewalMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CertificateRenewal>()
                        .orderByDesc(CertificateRenewal::getCreatedAt));
    }

    @Override
    public CertificateRenewal getById(Long id) {
        CertificateRenewal renewal = certificateRenewalMapper.selectById(id);
        if (renewal == null) throw new BusinessException("续期记录不存在");
        return renewal;
    }

    @Override
    @Transactional
    public CertificateRenewalRule createRule(CertificateRenewalRuleRequest req) {
        CertificateRenewalRule rule = CertificateRenewalRule.builder()
                .courseId(req.getCourseId())
                .rolePattern(req.getRolePattern() != null ? req.getRolePattern() : "*")
                .expiryThresholdDays(req.getExpiryThresholdDays() != null ? req.getExpiryThresholdDays() : 90)
                .minCourseVersion(req.getMinCourseVersion() != null ? req.getMinCourseVersion() : 1)
                .minRecentExamScore(req.getMinRecentExamScore())
                .recentExamWithinMonths(req.getRecentExamWithinMonths())
                .renewalAction(req.getRenewalAction())
                .description(req.getDescription())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .enabled(req.getEnabled() != null ? req.getEnabled() : 1)
                .build();
        certificateRenewalRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public CertificateRenewalRule updateRule(Long id, CertificateRenewalRuleRequest req) {
        CertificateRenewalRule rule = certificateRenewalRuleMapper.selectById(id);
        if (rule == null) throw new BusinessException("续期规则不存在");

        if (req.getRolePattern() != null) rule.setRolePattern(req.getRolePattern());
        if (req.getExpiryThresholdDays() != null) rule.setExpiryThresholdDays(req.getExpiryThresholdDays());
        if (req.getMinCourseVersion() != null) rule.setMinCourseVersion(req.getMinCourseVersion());
        if (req.getMinRecentExamScore() != null) rule.setMinRecentExamScore(req.getMinRecentExamScore());
        if (req.getRecentExamWithinMonths() != null) rule.setRecentExamWithinMonths(req.getRecentExamWithinMonths());
        if (req.getRenewalAction() != null) rule.setRenewalAction(req.getRenewalAction());
        if (req.getDescription() != null) rule.setDescription(req.getDescription());
        if (req.getSortOrder() != null) rule.setSortOrder(req.getSortOrder());
        if (req.getEnabled() != null) rule.setEnabled(req.getEnabled());
        certificateRenewalRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        CertificateRenewalRule rule = certificateRenewalRuleMapper.selectById(id);
        if (rule == null) throw new BusinessException("续期规则不存在");
        certificateRenewalRuleMapper.deleteById(id);
    }

    @Override
    public List<CertificateRenewalRule> listRules(Long courseId) {
        return certificateRenewalRuleMapper.selectList(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getCourseId, courseId)
                        .orderByAsc(CertificateRenewalRule::getSortOrder));
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private boolean matchesRule(CertificateRenewalRule rule, String studentRole,
                                 int courseVersion, BigDecimal recentExamScore) {
        // Check role pattern
        if (rule.getRolePattern() != null && !"*".equals(rule.getRolePattern())) {
            if (!rule.getRolePattern().equalsIgnoreCase(studentRole)) {
                return false;
            }
        }

        // Check course version
        if (rule.getMinCourseVersion() != null && courseVersion < rule.getMinCourseVersion()) {
            return false;
        }

        // Check recent exam score
        if (rule.getMinRecentExamScore() != null) {
            if (recentExamScore == null || recentExamScore.compareTo(rule.getMinRecentExamScore()) < 0) {
                return false;
            }
        }

        return true;
    }

    private BigDecimal getRecentExamScore(Long studentId, Long courseId, int withinMonths) {
        LocalDateTime since = LocalDateTime.now().minusMonths(withinMonths);
        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, studentId)
                        .eq(Grade::getCourseId, courseId)
                        .ge(Grade::getGradedAt, since)
                        .orderByDesc(Grade::getGradedAt)
                        .last("LIMIT 1"));

        if (grades.isEmpty()) return null;
        return grades.get(0).getScore() != null ? BigDecimal.valueOf(grades.get(0).getScore()) : null;
    }

    private Long findExamForCourse(Long courseId) {
        List<Exam> exams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getCourseId, courseId)
                        .eq(Exam::getStatus, "PUBLISHED")
                        .last("LIMIT 1"));
        return exams.isEmpty() ? null : exams.get(0).getId();
    }
}
