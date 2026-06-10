package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRevokeRequest;
import com.training.service.CertificateRenewalService;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.CertificateRenewalService;
import com.training.service.LearningRecordService;
import com.training.service.impl.CertificateServiceImpl;
import com.training.util.CertNoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Certificate Service Tests")
class CertificateServiceTest {

    @Mock private CertificateMapper certificateMapper;
    @Mock private CertificateRevocationMapper revocationMapper;
    @Mock private LearningRecordService learningRecordService;
    @Mock private GradeMapper gradeMapper;
    @Mock private ExamMapper examMapper;
    @Mock private CertNoGenerator certNoGenerator;
    @Mock private CourseMapper courseMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CertificateServiceImpl certificateService;

    private Certificate validCert;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(certificateService, "verifyTokenExpirationDays", 7);

        validCert = Certificate.builder()
                .id(1L)
                .studentId(1L)
                .courseId(10L)
                .certNo("CERT-10-20260610-0001")
                .title("Java Completion Certificate")
                .issueDate(LocalDate.of(2026, 6, 1))
                .status("VALID")
                .verifyToken("abc123token456")
                .verifyTokenExpiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    // ========================================================================
    // issue tests
    // ========================================================================

    @Nested
    @DisplayName("issue")
    class IssueTests {

        @Test
        @DisplayName("should create certificate when completion is 100% and exam is passed")
        void shouldSucceedWhenAllConditionsMet() {
            CertificateIssueRequest req = new CertificateIssueRequest();
            req.setStudentId(1L);
            req.setCourseId(10L);
            req.setTitle("Java Advanced Certificate");

            when(certificateMapper.selectOne(any())).thenReturn(null);
            when(learningRecordService.getCompletionRate(1L, 10L)).thenReturn(100.0);

            Grade passingGrade = Grade.builder().id(1L).studentId(1L).courseId(10L).pass(1).build();
            when(gradeMapper.selectList(any())).thenReturn(List.of(passingGrade));
            when(certNoGenerator.generate(10L)).thenReturn("CERT-10-20260610-0001");

            Certificate result = certificateService.issue(req, 1L);

            assertNotNull(result);
            assertEquals("VALID", result.getStatus());
            assertEquals("CERT-10-20260610-0001", result.getCertNo());
            assertEquals("Java Advanced Certificate", result.getTitle());
            assertNotNull(result.getVerifyToken());
            assertNotNull(result.getVerifyTokenExpiresAt());

            ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
            verify(certificateMapper).insert(captor.capture());
            assertEquals(1L, captor.getValue().getStudentId());
            assertEquals(10L, captor.getValue().getCourseId());
        }

        @Test
        @DisplayName("should throw when completion rate is below 100%")
        void shouldThrowWhenCompletionRateInsufficient() {
            CertificateIssueRequest req = new CertificateIssueRequest();
            req.setStudentId(1L);
            req.setCourseId(10L);

            when(certificateMapper.selectOne(any())).thenReturn(null);
            when(learningRecordService.getCompletionRate(1L, 10L)).thenReturn(75.5);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateService.issue(req, 1L));
            assertTrue(ex.getMessage().contains("完成率不足"));
            assertTrue(ex.getMessage().contains("75.5"));

            verify(certificateMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should throw when exam has not been passed")
        void shouldThrowWhenExamNotPassed() {
            CertificateIssueRequest req = new CertificateIssueRequest();
            req.setStudentId(1L);
            req.setCourseId(10L);

            when(certificateMapper.selectOne(any())).thenReturn(null);
            when(learningRecordService.getCompletionRate(1L, 10L)).thenReturn(100.0);
            when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateService.issue(req, 1L));
            assertTrue(ex.getMessage().contains("未通过"));

            verify(certificateMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should throw on duplicate certificate")
        void shouldThrowOnDuplicateCertificate() {
            CertificateIssueRequest req = new CertificateIssueRequest();
            req.setStudentId(1L);
            req.setCourseId(10L);

            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateService.issue(req, 1L));
            assertTrue(ex.getMessage().contains("已获得"));

            verify(learningRecordService, never()).getCompletionRate(anyLong(), anyLong());
            verify(gradeMapper, never()).selectList(any());
        }
    }

    // ========================================================================
    // revoke tests
    // ========================================================================

    @Nested
    @DisplayName("revoke")
    class RevokeTests {

        @Test
        @DisplayName("should update status to REVOKED and create revocation record")
        void shouldSucceed() {
            CertificateRevokeRequest req = new CertificateRevokeRequest();
            req.setReason("Academic dishonesty");

            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            certificateService.revoke(1L, req, 99L);

            ArgumentCaptor<Certificate> certCaptor = ArgumentCaptor.forClass(Certificate.class);
            verify(certificateMapper).updateById(certCaptor.capture());
            Certificate updated = certCaptor.getValue();
            assertEquals("REVOKED", updated.getStatus());
            assertEquals("Academic dishonesty", updated.getRevokeReason());
            assertEquals(99L, updated.getRevokedBy());
            assertNotNull(updated.getRevokedAt());

            ArgumentCaptor<CertificateRevocation> revCaptor = ArgumentCaptor.forClass(CertificateRevocation.class);
            verify(revocationMapper).insert(revCaptor.capture());
            assertEquals(1L, revCaptor.getValue().getCertificateId());
            assertEquals("Academic dishonesty", revCaptor.getValue().getReason());
            assertEquals(99L, revCaptor.getValue().getRevokedBy());
        }

        @Test
        @DisplayName("should throw when certificate is already revoked")
        void shouldThrowWhenAlreadyRevoked() {
            validCert.setStatus("REVOKED");

            CertificateRevokeRequest req = new CertificateRevokeRequest();
            req.setReason("test reason");

            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateService.revoke(1L, req, 1L));
            assertTrue(ex.getMessage().contains("已被撤销"));

            verify(certificateMapper, never()).updateById(any());
            verify(revocationMapper, never()).insert(any());
        }
    }

    // ========================================================================
    // verifyByCertNo tests
    // ========================================================================

    @Nested
    @DisplayName("verifyByCertNo")
    class VerifyByCertNoTests {

        @Test
        @DisplayName("should return valid=true for a valid certificate")
        void shouldReturnValidForValidCert() {
            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            Map<String, Object> result = certificateService.verifyByCertNo("CERT-10-20260610-0001");

            assertTrue((Boolean) result.get("valid"));
            assertEquals("证书有效", result.get("message"));
            assertEquals("CERT-10-20260610-0001", result.get("certNo"));
        }

        @Test
        @DisplayName("should return valid=false with revoke reason for revoked certificate")
        void shouldReturnInvalidForRevokedCert() {
            validCert.setStatus("REVOKED");
            validCert.setRevokeReason("Cheating during exam");
            validCert.setRevokedAt(LocalDateTime.of(2026, 6, 5, 10, 0));

            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            Map<String, Object> result = certificateService.verifyByCertNo("CERT-10-20260610-0001");

            assertFalse((Boolean) result.get("valid"));
            assertEquals("该证书已撤销", result.get("message"));
            assertEquals("Cheating during exam", result.get("revokeReason"));
            assertNotNull(result.get("revokedAt"));
        }

        @Test
        @DisplayName("should return valid=false for non-existent cert number")
        void shouldReturnInvalidForNonExistent() {
            when(certificateMapper.selectOne(any())).thenReturn(null);

            Map<String, Object> result = certificateService.verifyByCertNo("FAKE-CERT-999");

            assertFalse((Boolean) result.get("valid"));
            assertEquals("证书编号不存在", result.get("message"));
        }
    }

    // ========================================================================
    // verifyByToken tests (critical: revocation must be checked before token expiry)
    // ========================================================================

    @Nested
    @DisplayName("verifyByToken")
    class VerifyByTokenTests {

        @Test
        @DisplayName("should return valid=true for a valid unexpired token")
        void shouldReturnValidForValidToken() {
            validCert.setVerifyTokenExpiresAt(LocalDateTime.now().plusDays(5));

            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            Map<String, Object> result = certificateService.verifyByToken("abc123token456");

            assertTrue((Boolean) result.get("valid"));
            assertEquals("证书有效", result.get("message"));
            assertEquals("CERT-10-20260610-0001", result.get("certNo"));
        }

        @Test
        @DisplayName("should return valid=false with expired flag for expired token on valid cert")
        void shouldReturnExpiredForExpiredTokenOnValidCert() {
            validCert.setVerifyTokenExpiresAt(LocalDateTime.now().minusDays(1));

            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            Map<String, Object> result = certificateService.verifyByToken("abc123token456");

            assertFalse((Boolean) result.get("valid"));
            assertTrue((Boolean) result.get("expired"));
            assertEquals("验证链接已过期，请联系管理员重新生成", result.get("message"));
        }

        @Test
        @DisplayName("should return valid=false for non-existent token")
        void shouldReturnInvalidForNonExistentToken() {
            when(certificateMapper.selectOne(any())).thenReturn(null);

            Map<String, Object> result = certificateService.verifyByToken("nonexistent-token");

            assertFalse((Boolean) result.get("valid"));
            assertEquals("验证链接无效", result.get("message"));
        }

        @Test
        @DisplayName("CRITICAL: revoked cert with expired token must show REVOKED, not expired")
        void revokedCertWithExpiredTokenMustShowRevoked() {
            // This is the critical scenario: cert is revoked AND token has expired.
            // The verify endpoint must show "revoked" status, not "token expired".
            // Otherwise, someone with a revoked cert could think it's just a token issue
            // and try to regenerate — which must be blocked.
            validCert.setStatus("REVOKED");
            validCert.setRevokeReason("Academic dishonesty");
            validCert.setRevokedAt(LocalDateTime.of(2026, 6, 5, 10, 0));
            validCert.setVerifyTokenExpiresAt(LocalDateTime.now().minusDays(10)); // expired token

            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            Map<String, Object> result = certificateService.verifyByToken("abc123token456");

            // Must show revoked, NOT expired
            assertFalse((Boolean) result.get("valid"));
            assertEquals("该证书已撤销", result.get("message"));
            assertEquals("Academic dishonesty", result.get("revokeReason"));
            assertNotNull(result.get("revokedAt"));

            // Must NOT show as "expired"
            assertNull(result.get("expired"));
        }

        @Test
        @DisplayName("revoked cert with valid token must also show REVOKED")
        void revokedCertWithValidTokenMustShowRevoked() {
            validCert.setStatus("REVOKED");
            validCert.setRevokeReason("Fraud");
            validCert.setRevokedAt(LocalDateTime.of(2026, 6, 8, 14, 0));
            validCert.setVerifyTokenExpiresAt(LocalDateTime.now().plusDays(5)); // still valid

            when(certificateMapper.selectOne(any())).thenReturn(validCert);

            Map<String, Object> result = certificateService.verifyByToken("abc123token456");

            assertFalse((Boolean) result.get("valid"));
            assertEquals("该证书已撤销", result.get("message"));
            assertEquals("Fraud", result.get("revokeReason"));
        }
    }

    // ========================================================================
    // regenerateVerifyToken tests
    // ========================================================================

    @Nested
    @DisplayName("regenerateVerifyToken")
    class RegenerateTokenTests {

        @Test
        @DisplayName("should generate new token for valid certificate")
        void shouldGenerateNewTokenForValidCert() {
            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            String newToken = certificateService.regenerateVerifyToken(1L);

            assertNotNull(newToken);
            assertNotEquals("abc123token456", newToken);
            verify(certificateMapper).updateById(any(Certificate.class));
        }

        @Test
        @DisplayName("CRITICAL: should throw for revoked certificate — prevent re-enabling verify link")
        void shouldThrowForRevokedCert() {
            validCert.setStatus("REVOKED");
            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> certificateService.regenerateVerifyToken(1L));
            assertTrue(ex.getMessage().contains("已撤销"));

            // Must NOT update the certificate
            verify(certificateMapper, never()).updateById(any());
        }
    }

    // ========================================================================
    // Renewal Blocking tests (integration point: revoked cert blocks renewal)
    // ========================================================================

    @Nested
    @DisplayName("Renewal Blocking (撤销证书拦截续期)")
    class RenewalBlockingTests {

        @Mock
        private CertificateRenewalService certificateRenewalService;

        @Test
        @DisplayName("revoked certificate status should persist correctly for renewal check")
        void revokedStatusShouldPersistForRenewalCheck() {
            CertificateRevokeRequest req = new CertificateRevokeRequest();
            req.setReason("Compliance violation");

            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            certificateService.revoke(1L, req, 99L);

            // Verify the cert status is now REVOKED in the update call
            ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
            verify(certificateMapper).updateById(captor.capture());
            Certificate revokedCert = captor.getValue();

            assertEquals("REVOKED", revokedCert.getStatus());
            assertNotNull(revokedCert.getRevokedAt());
            assertNotNull(revokedCert.getRevokeReason());
            // This is the state that CertificateRenewalService checks to block renewal
        }

        @Test
        @DisplayName("revoked certificate should have audit log entry for revocation")
        void revokedCertShouldHaveAuditLog() {
            CertificateRevokeRequest req = new CertificateRevokeRequest();
            req.setReason("Data breach");

            when(certificateMapper.selectById(1L)).thenReturn(validCert);

            certificateService.revoke(1L, req, 99L);

            // Audit log should record the revocation
            verify(auditLogService).log(eq(99L), isNull(), eq("CERT_STATUS_CHANGED"),
                    eq("CERTIFICATE"), eq(1L), contains("REVOKE"));
        }

        @Test
        @DisplayName("issue should track course version for future renewal evaluation")
        void issueShouldTrackCourseVersion() {
            CertificateIssueRequest req = new CertificateIssueRequest();
            req.setStudentId(1L);
            req.setCourseId(10L);
            req.setTitle("Test Cert");

            Course course = Course.builder().id(10L).version(3).build();

            when(certificateMapper.selectOne(any())).thenReturn(null);
            when(learningRecordService.getCompletionRate(1L, 10L)).thenReturn(100.0);
            Grade passingGrade = Grade.builder().id(1L).studentId(1L).courseId(10L).pass(1).build();
            when(gradeMapper.selectList(any())).thenReturn(List.of(passingGrade));
            when(certNoGenerator.generate(10L)).thenReturn("CERT-10-20260610-0002");
            when(courseMapper.selectById(10L)).thenReturn(course);

            Certificate result = certificateService.issue(req, 1L);

            // courseVersion should be tracked for renewal version-change detection
            assertEquals(3, result.getCourseVersion());
        }
    }
}
