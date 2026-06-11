package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRenewalRuleRequest;
import com.training.entity.dto.RenewalAssessmentResult;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.CertificateService;
import com.training.service.LearningPathService;
import com.training.service.MakeupExamService;
import com.training.service.impl.CertificateRenewalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Certificate Renewal Service Tests")
class CertificateRenewalServiceTest {

    @Mock private CertificateRenewalMapper certificateRenewalMapper;
    @Mock private CertificateRenewalRuleMapper certificateRenewalRuleMapper;
    @Mock private CertificateMapper certificateMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private GradeMapper gradeMapper;
    @Mock private ExamMapper examMapper;
    @Mock private CertificateService certificateService;
    @Mock private LearningPathService learningPathService;
    @Mock private MakeupExamService makeupExamService;
    @Mock private AuditLogService auditLogService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CertificateRenewalServiceImpl certificateRenewalService;

    private Certificate validCert;
    private Certificate revokedCert;
    private SysUser student;
    private Course course;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        validCert = Certificate.builder()
                .id(1L).studentId(1L).courseId(10L).certNo("CERT-10")
                .status("VALID")
                .expiryDate(LocalDate.now().plusDays(30))
                .title("Test Cert")
                .build();

        revokedCert = Certificate.builder()
                .id(1L).studentId(1L).courseId(10L).certNo("CERT-10")
                .status("REVOKED")
                .expiryDate(LocalDate.now().plusDays(30))
                .title("Test Cert")
                .build();

        student = SysUser.builder().id(1L).role("STUDENT").build();
        course = Course.builder().id(10L).version(2).build();
    }

    // ================================================================
    // Helper: set up the mocks that assessRenewal always needs
    // ================================================================

    private void setupAssessRenewalBaseMocks() {
        when(certificateMapper.selectById(1L)).thenReturn(validCert);
        when(sysUserMapper.selectById(1L)).thenReturn(student);
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    // ========================================================================
    // assessRenewal
    // ========================================================================

    @Nested
    @DisplayName("assessRenewal")
    class AssessRenewalTests {

        @Test
        @DisplayName("CRITICAL: should throw when cert REVOKED")
        void shouldThrowWhenCertRevoked() {
            when(certificateMapper.selectById(1L)).thenReturn(revokedCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.assessRenewal(1L));
            assertTrue(ex.getMessage().contains("证书已撤销"));

            verify(sysUserMapper, never()).selectById(anyLong());
            verify(courseMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("should throw when expired more than 1 year")
        void shouldThrowWhenExpiredMoreThanOneYear() {
            Certificate expiredCert = Certificate.builder()
                    .id(1L).studentId(1L).courseId(10L).certNo("CERT-10")
                    .status("EXPIRED")
                    .expiryDate(LocalDate.now().minusYears(2))
                    .title("Test Cert")
                    .build();
            when(certificateMapper.selectById(1L)).thenReturn(expiredCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.assessRenewal(1L));
            assertTrue(ex.getMessage().contains("证书已过期超过1年"));
        }

        @Test
        @DisplayName("should return DIRECT_RENEWAL when rule matches")
        void shouldReturnDirectRenewalWhenRuleMatches() {
            setupAssessRenewalBaseMocks();

            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(1).enabled(1)
                    .description("Direct renewal for matching students")
                    .build();
            when(certificateRenewalRuleMapper.selectList(any())).thenReturn(List.of(rule));

            RenewalAssessmentResult result = certificateRenewalService.assessRenewal(1L);

            assertEquals("DIRECT_RENEWAL", result.getRenewalAction());
            assertEquals(100L, result.getMatchedRuleId());
            assertEquals(1L, result.getCertificateId());
            assertEquals(1L, result.getStudentId());
            assertEquals(10L, result.getCourseId());
            assertEquals("Direct renewal for matching students", result.getMatchedRuleDescription());
        }

        @Test
        @DisplayName("should return MAKEUP_EXAM when recent score below threshold")
        void shouldReturnMakeupExamWhenScoreBelowThreshold() {
            setupAssessRenewalBaseMocks();

            // Student scored 50 -- below the 80 threshold
            Grade lowGrade = Grade.builder()
                    .id(1L).studentId(1L).courseId(10L)
                    .score(50.0)
                    .gradedAt(LocalDateTime.now().minusMonths(1))
                    .build();
            when(gradeMapper.selectList(any())).thenReturn(List.of(lowGrade));

            // Rule 1: DIRECT_RENEWAL needs minRecentExamScore=80 (won't match score 50)
            CertificateRenewalRule directRule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .minRecentExamScore(BigDecimal.valueOf(80))
                    .sortOrder(1).enabled(1)
                    .description("Direct renewal requires high score")
                    .build();

            // Rule 2: MAKEUP_EXAM has no score requirement (will match)
            CertificateRenewalRule makeupRule = CertificateRenewalRule.builder()
                    .id(101L).courseId(10L)
                    .renewalAction("MAKEUP_EXAM")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(2).enabled(1)
                    .description("Makeup exam for low scores")
                    .build();

            when(certificateRenewalRuleMapper.selectList(any()))
                    .thenReturn(List.of(directRule, makeupRule));

            RenewalAssessmentResult result = certificateRenewalService.assessRenewal(1L);

            assertEquals("MAKEUP_EXAM", result.getRenewalAction());
            assertEquals(101L, result.getMatchedRuleId());
        }

        @Test
        @DisplayName("should return FULL_RELEARN as default when no rules match")
        void shouldReturnFullRelearnAsDefault() {
            setupAssessRenewalBaseMocks();
            when(certificateRenewalRuleMapper.selectList(any())).thenReturn(Collections.emptyList());

            RenewalAssessmentResult result = certificateRenewalService.assessRenewal(1L);

            assertEquals("FULL_RELEARN", result.getRenewalAction());
            assertEquals(1L, result.getCertificateId());
            assertEquals(1L, result.getStudentId());
            assertEquals(10L, result.getCourseId());
            assertTrue(result.getReason().contains("未匹配任何续期规则"));
            assertNull(result.getMatchedRuleId());
        }

        @Test
        @DisplayName("should evaluate rules in sortOrder")
        void shouldEvaluateRulesInSortOrder() {
            setupAssessRenewalBaseMocks();

            // Two rules already sorted by sortOrder; the first one matches
            CertificateRenewalRule firstRule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(1).enabled(1)
                    .description("First matching rule")
                    .build();

            CertificateRenewalRule secondRule = CertificateRenewalRule.builder()
                    .id(101L).courseId(10L)
                    .renewalAction("MAKEUP_EXAM")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(2).enabled(1)
                    .description("Second rule should not be reached")
                    .build();

            when(certificateRenewalRuleMapper.selectList(any()))
                    .thenReturn(List.of(firstRule, secondRule));

            RenewalAssessmentResult result = certificateRenewalService.assessRenewal(1L);

            // First rule wins because it matches and comes first in sort order
            assertEquals("DIRECT_RENEWAL", result.getRenewalAction());
            assertEquals(100L, result.getMatchedRuleId());
        }

        @Test
        @DisplayName("should match student role against rolePattern")
        void shouldMatchStudentRoleAgainstRolePattern() {
            setupAssessRenewalBaseMocks();

            // Rule requires INSTRUCTOR role, but student role is STUDENT
            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("INSTRUCTOR")
                    .minCourseVersion(1)
                    .sortOrder(1).enabled(1)
                    .description("Instructor-only rule")
                    .build();

            when(certificateRenewalRuleMapper.selectList(any())).thenReturn(List.of(rule));

            RenewalAssessmentResult result = certificateRenewalService.assessRenewal(1L);

            // Role mismatch -> rule does not match -> default FULL_RELEARN
            assertEquals("FULL_RELEARN", result.getRenewalAction());
            assertNull(result.getMatchedRuleId());
        }
    }

    // ========================================================================
    // initiateRenewal
    // ========================================================================

    @Nested
    @DisplayName("initiateRenewal")
    class InitiateRenewalTests {

        @Test
        @DisplayName("DIRECT_RENEWAL should issue new cert")
        void directRenewalShouldIssueNewCert() {
            // Redis lock succeeds
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            // assessRenewal prerequisites
            setupAssessRenewalBaseMocks();
            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(1).enabled(1)
                    .description("Direct renewal")
                    .build();
            when(certificateRenewalRuleMapper.selectList(any())).thenReturn(List.of(rule));

            // No existing active renewal
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            // certificateService.issue returns a brand-new certificate
            Certificate newCert = Certificate.builder().id(200L).build();
            when(certificateService.issue(any(CertificateIssueRequest.class), eq(1L)))
                    .thenReturn(newCert);

            CertificateRenewal result = certificateRenewalService.initiateRenewal(1L, 1L);

            // Verify the returned renewal
            assertNotNull(result);
            assertEquals("COMPLETED", result.getStatus());
            assertEquals(200L, result.getNewCertificateId());
            assertEquals("DIRECT_RENEWAL", result.getRenewalAction());
            assertEquals(1L, result.getProcessedBy());
            assertNotNull(result.getProcessedAt());

            // Verify insert captured the correct renewal
            ArgumentCaptor<CertificateRenewal> renewalCaptor = ArgumentCaptor.forClass(CertificateRenewal.class);
            verify(certificateRenewalMapper).insert(renewalCaptor.capture());
            CertificateRenewal inserted = renewalCaptor.getValue();
            assertEquals(1L, inserted.getCertificateId());
            assertEquals(200L, inserted.getNewCertificateId());
            assertEquals("COMPLETED", inserted.getStatus());
            assertEquals(100L, inserted.getRenewalRuleId());

            // Verify certificateService.issue was called with correct params
            ArgumentCaptor<CertificateIssueRequest> issueCaptor =
                    ArgumentCaptor.forClass(CertificateIssueRequest.class);
            verify(certificateService).issue(issueCaptor.capture(), eq(1L));
            assertEquals(1L, issueCaptor.getValue().getStudentId());
            assertEquals(10L, issueCaptor.getValue().getCourseId());
            assertEquals("Test Cert", issueCaptor.getValue().getTitle());

            // Verify audit log
            verify(auditLogService).log(eq("RENEWAL_INITIATED"), eq("CERTIFICATE_RENEWAL"),
                    any(), eq(1L), isNull(), any());
        }

        @Test
        @DisplayName("should throw when renewal already in progress")
        void shouldThrowWhenRenewalAlreadyInProgress() {
            // Redis lock succeeds
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            // cert is valid (re-check inside lock)
            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            // assessRenewal prerequisites
            setupAssessRenewalBaseMocks();
            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(1).enabled(1)
                    .build();
            when(certificateRenewalRuleMapper.selectList(any())).thenReturn(List.of(rule));

            // Existing active renewal blocks the new one
            CertificateRenewal existingRenewal = CertificateRenewal.builder()
                    .id(50L).certificateId(1L).status("IN_PROGRESS").build();
            when(certificateRenewalMapper.selectOne(any())).thenReturn(existingRenewal);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.initiateRenewal(1L, 1L));
            assertTrue(ex.getMessage().contains("已存在进行中的续期记录"));

            // No new renewal should have been inserted
            verify(certificateRenewalMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should use Redis lock with post-commit release")
        void shouldUseRedisLock() {
            // Redis lock succeeds
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            // cert is valid
            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            // assessRenewal prerequisites
            setupAssessRenewalBaseMocks();
            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .renewalAction("DIRECT_RENEWAL")
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .sortOrder(1).enabled(1)
                    .build();
            when(certificateRenewalRuleMapper.selectList(any())).thenReturn(List.of(rule));
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            Certificate newCert = Certificate.builder().id(200L).build();
            when(certificateService.issue(any(CertificateIssueRequest.class), eq(1L)))
                    .thenReturn(newCert);

            certificateRenewalService.initiateRenewal(1L, 1L);

            // Verify setIfAbsent was called with the correct lock key and TTL
            ArgumentCaptor<String> lockKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
            verify(valueOperations).setIfAbsent(
                    lockKeyCaptor.capture(), eq("1"), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
            assertEquals("renewal:lock:1", lockKeyCaptor.getValue());
            assertEquals(60L, ttlCaptor.getValue());
            // In unit tests (no active synchronization), lock auto-expires via TTL
        }

        @Test
        @DisplayName("CRITICAL: should re-check revocation status inside lock to prevent race with concurrent revoke")
        void shouldReCheckRevocationInsideLock() {
            // Redis lock succeeds
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

            // Cert was revoked between the time user clicked "renew" and lock acquisition
            when(certificateMapper.selectById(1L)).thenReturn(revokedCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.initiateRenewal(1L, 1L));
            assertTrue(ex.getMessage().contains("证书已撤销"));

            // Should NOT proceed to assessRenewal or insert
            verify(certificateRenewalMapper, never()).insert(any());
            verify(sysUserMapper, never()).selectById(anyLong());
        }
    }

    // ========================================================================
    // completeRenewal
    // ========================================================================

    @Nested
    @DisplayName("completeRenewal")
    class CompleteRenewalTests {

        @Test
        @DisplayName("should issue new certificate and set COMPLETED")
        void shouldIssueNewCertificateAndSetCompleted() {
            CertificateRenewal renewal = CertificateRenewal.builder()
                    .id(50L).certificateId(1L)
                    .studentId(1L).courseId(10L)
                    .status("IN_PROGRESS")
                    .build();
            when(certificateRenewalMapper.selectById(50L)).thenReturn(renewal);
            when(certificateMapper.selectById(1L)).thenReturn(validCert);
            when(courseMapper.selectById(10L)).thenReturn(course);

            Certificate newCert = Certificate.builder().id(300L).build();
            when(certificateService.issue(any(CertificateIssueRequest.class), eq(99L)))
                    .thenReturn(newCert);

            certificateRenewalService.completeRenewal(50L, 99L);

            // Verify the renewal was updated to COMPLETED
            ArgumentCaptor<CertificateRenewal> renewalCaptor =
                    ArgumentCaptor.forClass(CertificateRenewal.class);
            verify(certificateRenewalMapper).updateById(renewalCaptor.capture());
            CertificateRenewal updated = renewalCaptor.getValue();
            assertEquals("COMPLETED", updated.getStatus());
            assertEquals(300L, updated.getNewCertificateId());
            assertEquals(99L, updated.getProcessedBy());
            assertNotNull(updated.getProcessedAt());

            // Verify certificateService.issue was called with correct params
            ArgumentCaptor<CertificateIssueRequest> issueCaptor =
                    ArgumentCaptor.forClass(CertificateIssueRequest.class);
            verify(certificateService).issue(issueCaptor.capture(), eq(99L));
            assertEquals(1L, issueCaptor.getValue().getStudentId());
            assertEquals(10L, issueCaptor.getValue().getCourseId());
            assertEquals("Test Cert", issueCaptor.getValue().getTitle());

            // Verify audit log
            verify(auditLogService).log(eq("RENEWAL_COMPLETED"), eq("CERTIFICATE_RENEWAL"),
                    eq(50L), eq(99L), isNull(), any());
        }

        @Test
        @DisplayName("should throw when not IN_PROGRESS")
        void shouldThrowWhenNotInProgress() {
            CertificateRenewal renewal = CertificateRenewal.builder()
                    .id(50L).certificateId(1L)
                    .studentId(1L).courseId(10L)
                    .status("COMPLETED")
                    .build();
            when(certificateRenewalMapper.selectById(50L)).thenReturn(renewal);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.completeRenewal(50L, 99L));
            assertTrue(ex.getMessage().contains("续期记录状态不允许完成"));

            verify(certificateRenewalMapper, never()).updateById(any());
            verify(certificateService, never()).issue(any(), anyLong());
        }

        @Test
        @DisplayName("CRITICAL: should reject completion when certificate was revoked after renewal initiated")
        void shouldRejectCompletionWhenCertRevokedDuringRenewal() {
            CertificateRenewal renewal = CertificateRenewal.builder()
                    .id(50L).certificateId(1L)
                    .studentId(1L).courseId(10L)
                    .status("IN_PROGRESS")
                    .build();
            when(certificateRenewalMapper.selectById(50L)).thenReturn(renewal);

            // Certificate was revoked after renewal was initiated
            when(certificateMapper.selectById(1L)).thenReturn(revokedCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateRenewalService.completeRenewal(50L, 99L));
            assertTrue(ex.getMessage().contains("证书已被撤销"));

            // Verify the renewal was auto-rejected
            ArgumentCaptor<CertificateRenewal> captor = ArgumentCaptor.forClass(CertificateRenewal.class);
            verify(certificateRenewalMapper).updateById(captor.capture());
            CertificateRenewal rejected = captor.getValue();
            assertEquals("REJECTED", rejected.getStatus());
            assertTrue(rejected.getRejectionReason().contains("撤销"));

            // No new certificate should be issued
            verify(certificateService, never()).issue(any(), anyLong());
        }

        @Test
        @DisplayName("should complete renewal even when course version upgraded (with warning log)")
        void shouldCompleteRenewalWhenCourseVersionUpgraded() {
            CertificateRenewal renewal = CertificateRenewal.builder()
                    .id(50L).certificateId(1L)
                    .studentId(1L).courseId(10L)
                    .renewalRuleId(100L)
                    .status("IN_PROGRESS")
                    .build();
            when(certificateRenewalMapper.selectById(50L)).thenReturn(renewal);
            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            // Course version upgraded from 2 to 5 during renewal
            Course upgradedCourse = Course.builder().id(10L).version(5).build();
            when(courseMapper.selectById(10L)).thenReturn(upgradedCourse);

            // Rule had minCourseVersion=2
            CertificateRenewalRule rule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .minCourseVersion(2)
                    .renewalAction("MAKEUP_EXAM")
                    .build();
            when(certificateRenewalRuleMapper.selectById(100L)).thenReturn(rule);

            Certificate newCert = Certificate.builder().id(400L).build();
            when(certificateService.issue(any(CertificateIssueRequest.class), eq(99L)))
                    .thenReturn(newCert);

            // Should still complete (version mismatch is logged as warning, not blocking)
            certificateRenewalService.completeRenewal(50L, 99L);

            ArgumentCaptor<CertificateRenewal> captor = ArgumentCaptor.forClass(CertificateRenewal.class);
            verify(certificateRenewalMapper).updateById(captor.capture());
            assertEquals("COMPLETED", captor.getValue().getStatus());
            assertEquals(400L, captor.getValue().getNewCertificateId());
        }
    }

    // ========================================================================
    // rejectRenewal
    // ========================================================================

    @Nested
    @DisplayName("rejectRenewal")
    class RejectRenewalTests {

        @Test
        @DisplayName("should set REJECTED with reason")
        void shouldSetRejectedWithReason() {
            CertificateRenewal renewal = CertificateRenewal.builder()
                    .id(50L).certificateId(1L)
                    .studentId(1L).courseId(10L)
                    .status("IN_PROGRESS")
                    .build();
            when(certificateRenewalMapper.selectById(50L)).thenReturn(renewal);

            certificateRenewalService.rejectRenewal(50L, "材料不完整", 99L);

            ArgumentCaptor<CertificateRenewal> captor =
                    ArgumentCaptor.forClass(CertificateRenewal.class);
            verify(certificateRenewalMapper).updateById(captor.capture());
            CertificateRenewal updated = captor.getValue();
            assertEquals("REJECTED", updated.getStatus());
            assertEquals("材料不完整", updated.getRejectionReason());
            assertEquals(99L, updated.getProcessedBy());
            assertNotNull(updated.getProcessedAt());

            // Verify audit log
            verify(auditLogService).log(eq("RENEWAL_REJECTED"), eq("CERTIFICATE_RENEWAL"),
                    eq(50L), eq(99L), isNull(), any());
        }
    }

    // ========================================================================
    // RuleCrud
    // ========================================================================

    @Nested
    @DisplayName("RuleCrud")
    class RuleCrudTests {

        @Test
        @DisplayName("should create rule")
        void shouldCreateRule() {
            CertificateRenewalRuleRequest req = new CertificateRenewalRuleRequest();
            req.setCourseId(10L);
            req.setRolePattern("*");
            req.setMinCourseVersion(1);
            req.setRenewalAction("DIRECT_RENEWAL");
            req.setDescription("Test rule");
            req.setSortOrder(1);
            req.setEnabled(1);

            CertificateRenewalRule result = certificateRenewalService.createRule(req);

            assertNotNull(result);
            assertEquals(10L, result.getCourseId());
            assertEquals("DIRECT_RENEWAL", result.getRenewalAction());
            assertEquals("*", result.getRolePattern());

            ArgumentCaptor<CertificateRenewalRule> captor =
                    ArgumentCaptor.forClass(CertificateRenewalRule.class);
            verify(certificateRenewalRuleMapper).insert(captor.capture());
            CertificateRenewalRule inserted = captor.getValue();
            assertEquals(10L, inserted.getCourseId());
            assertEquals("*", inserted.getRolePattern());
            assertEquals(1, inserted.getMinCourseVersion());
            assertEquals("DIRECT_RENEWAL", inserted.getRenewalAction());
            assertEquals("Test rule", inserted.getDescription());
            assertEquals(1, inserted.getSortOrder());
            assertEquals(1, inserted.getEnabled());
        }

        @Test
        @DisplayName("should update rule")
        void shouldUpdateRule() {
            CertificateRenewalRule existingRule = CertificateRenewalRule.builder()
                    .id(100L).courseId(10L)
                    .rolePattern("*")
                    .minCourseVersion(1)
                    .renewalAction("DIRECT_RENEWAL")
                    .description("Old description")
                    .sortOrder(1).enabled(1)
                    .build();
            when(certificateRenewalRuleMapper.selectById(100L)).thenReturn(existingRule);

            CertificateRenewalRuleRequest req = new CertificateRenewalRuleRequest();
            req.setRenewalAction("MAKEUP_EXAM");
            req.setDescription("Updated description");

            CertificateRenewalRule result = certificateRenewalService.updateRule(100L, req);

            assertNotNull(result);
            assertEquals("MAKEUP_EXAM", result.getRenewalAction());
            assertEquals("Updated description", result.getDescription());

            ArgumentCaptor<CertificateRenewalRule> captor =
                    ArgumentCaptor.forClass(CertificateRenewalRule.class);
            verify(certificateRenewalRuleMapper).updateById(captor.capture());
            CertificateRenewalRule updated = captor.getValue();
            assertEquals(100L, updated.getId());
            assertEquals("MAKEUP_EXAM", updated.getRenewalAction());
            assertEquals("Updated description", updated.getDescription());
            // Unchanged fields should remain as they were
            assertEquals(10L, updated.getCourseId());
            assertEquals("*", updated.getRolePattern());
            assertEquals(1, updated.getMinCourseVersion());
        }
    }
}
