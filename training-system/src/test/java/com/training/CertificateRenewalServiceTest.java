package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.LearningPathService;
import com.training.service.impl.CertificateRenewalServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Certificate Renewal Service Tests")
class CertificateRenewalServiceTest {

    @Mock private CertificateRenewalMapper certificateRenewalMapper;
    @Mock private CertificateMapper certificateMapper;
    @Mock private CertificateRenewalRuleMapper renewalRuleMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private GradeMapper gradeMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private LearningPathService learningPathService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CertificateRenewalServiceImpl certificateRenewalService;

    @Nested
    @DisplayName("evaluateRenewal")
    class EvaluateRenewalTests {

        @Test
        @DisplayName("CRITICAL: should block renewal for revoked certificate (证书撤销后续期拦截)")
        void shouldBlockRenewalForRevokedCertificate() {
            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).status("REVOKED")
                    .revokeReason("Academic dishonesty").build();
            when(certificateMapper.selectById(1L)).thenReturn(cert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.evaluateRenewal(1L, 99L));
            assertTrue(ex.getMessage().contains("已撤销"));

            // No renewal record should be created
            verify(certificateRenewalMapper, never()).insert(any());
            // No certificate update should happen
            verify(certificateMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("should direct renew for exempted role")
        void shouldDirectRenewForExemptedRole() {
            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).courseVersion(1)
                    .status("VALID").expiryDate(LocalDate.now().plusDays(10)).build();
            when(certificateMapper.selectById(1L)).thenReturn(cert);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .courseId(10L).renewalPeriodDays(365).advanceNoticeDays(30)
                    .requireExamPass(1).minExamScore(80)
                    .versionChangePolicy("RELEARN").roleExemptions("[\"ADMIN\"]")
                    .enabled(1).build();
            when(renewalRuleMapper.selectOne(any())).thenReturn(rule);

            Course course = Course.builder().id(10L).version(1).build();
            when(courseMapper.selectById(10L)).thenReturn(course);

            SysUser student = SysUser.builder().id(1L).role("ADMIN").build();
            when(sysUserMapper.selectById(1L)).thenReturn(student);

            CertificateRenewal result = certificateRenewalService.evaluateRenewal(1L, 99L);

            assertEquals("DIRECT_RENEW", result.getDecision());
            assertEquals("COMPLETED", result.getStatus());
            assertNotNull(result.getNewExpiryDate());

            verify(certificateMapper).updateById(argThat(c -> "VALID".equals(c.getStatus())));
        }

        @Test
        @DisplayName("CRITICAL: should require relearn when course version changed (课程版本升级测试)")
        void shouldRequireRelearnWhenCourseVersionChanged() {
            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).courseVersion(1)
                    .status("VALID").expiryDate(LocalDate.now().plusDays(10)).build();
            when(certificateMapper.selectById(1L)).thenReturn(cert);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .courseId(10L).renewalPeriodDays(365).advanceNoticeDays(30)
                    .versionChangePolicy("RELEARN").enabled(1).build();
            when(renewalRuleMapper.selectOne(any())).thenReturn(rule);

            // Course version changed from 1 to 2
            Course course = Course.builder().id(10L).version(2).build();
            when(courseMapper.selectById(10L)).thenReturn(course);

            SysUser student = SysUser.builder().id(1L).role("STUDENT").build();
            when(sysUserMapper.selectById(1L)).thenReturn(student);

            CertificateRenewal result = certificateRenewalService.evaluateRenewal(1L, 99L);

            assertEquals("RELEARN", result.getDecision());
            assertEquals("PENDING", result.getStatus());
            assertEquals(1, result.getCourseVersionAtIssue());
            assertEquals(2, result.getCourseVersionCurrent());

            // Learning path should be generated
            verify(learningPathService).generatePath(1L, 10L, "RENEWAL");
        }

        @Test
        @DisplayName("should require makeup exam when score below minimum")
        void shouldRequireMakeupExamWhenScoreBelowMinimum() {
            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).courseVersion(1)
                    .status("VALID").expiryDate(LocalDate.now().plusDays(10)).build();
            when(certificateMapper.selectById(1L)).thenReturn(cert);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .courseId(10L).renewalPeriodDays(365).advanceNoticeDays(30)
                    .requireExamPass(1).minExamScore(80)
                    .versionChangePolicy("RELEARN").enabled(1).build();
            when(renewalRuleMapper.selectOne(any())).thenReturn(rule);

            Course course = Course.builder().id(10L).version(1).build();
            when(courseMapper.selectById(10L)).thenReturn(course);

            SysUser student = SysUser.builder().id(1L).role("STUDENT").build();
            when(sysUserMapper.selectById(1L)).thenReturn(student);

            // Passed but below minExamScore
            Grade grade = Grade.builder().studentId(1L).courseId(10L).score(65.0).pass(1)
                    .gradedAt(LocalDateTime.now()).build();
            when(gradeMapper.selectList(any())).thenReturn(List.of(grade));

            CertificateRenewal result = certificateRenewalService.evaluateRenewal(1L, 99L);

            assertEquals("MAKEUP_EXAM", result.getDecision());
            assertEquals("PENDING", result.getStatus());
        }

        @Test
        @DisplayName("should direct renew when course version changed but policy allows")
        void shouldDirectRenewWhenPolicyAllows() {
            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).courseVersion(1)
                    .status("VALID").expiryDate(LocalDate.now().plusDays(10)).build();
            when(certificateMapper.selectById(1L)).thenReturn(cert);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .courseId(10L).renewalPeriodDays(365).advanceNoticeDays(30)
                    .versionChangePolicy("DIRECT_RENEW").enabled(1).build();
            when(renewalRuleMapper.selectOne(any())).thenReturn(rule);

            Course course = Course.builder().id(10L).version(2).build();
            when(courseMapper.selectById(10L)).thenReturn(course);

            SysUser student = SysUser.builder().id(1L).role("STUDENT").build();
            when(sysUserMapper.selectById(1L)).thenReturn(student);

            CertificateRenewal result = certificateRenewalService.evaluateRenewal(1L, 99L);

            assertEquals("DIRECT_RENEW", result.getDecision());
            assertEquals("COMPLETED", result.getStatus());
        }

        @Test
        @DisplayName("should require relearn when no exam records exist")
        void shouldRequireRelearnWhenNoExamRecords() {
            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).courseVersion(1)
                    .status("VALID").expiryDate(LocalDate.now().plusDays(10)).build();
            when(certificateMapper.selectById(1L)).thenReturn(cert);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .courseId(10L).renewalPeriodDays(365).advanceNoticeDays(30)
                    .versionChangePolicy("RELEARN").enabled(1).build();
            when(renewalRuleMapper.selectOne(any())).thenReturn(rule);

            Course course = Course.builder().id(10L).version(1).build();
            when(courseMapper.selectById(10L)).thenReturn(course);

            SysUser student = SysUser.builder().id(1L).role("STUDENT").build();
            when(sysUserMapper.selectById(1L)).thenReturn(student);

            when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList());

            CertificateRenewal result = certificateRenewalService.evaluateRenewal(1L, 99L);

            assertEquals("RELEARN", result.getDecision());
        }
    }

    @Nested
    @DisplayName("scanExpiringCertificates")
    class ScanTests {

        @Test
        @DisplayName("should skip certificates with existing pending renewal")
        void shouldSkipCertificatesWithPendingRenewal() {
            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .courseId(10L).advanceNoticeDays(30).renewalPeriodDays(365).enabled(1).build();
            when(renewalRuleMapper.selectList(any())).thenReturn(List.of(rule));

            Certificate cert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).courseVersion(1)
                    .status("VALID").expiryDate(LocalDate.now().plusDays(10)).build();
            when(certificateMapper.selectList(any())).thenReturn(List.of(cert));

            // Already has pending renewal
            when(certificateRenewalMapper.selectCount(any())).thenReturn(1L);

            certificateRenewalService.scanExpiringCertificates();

            // Should not create a new renewal evaluation
            verify(certificateRenewalMapper, never()).insert(any());
        }
    }
}
