package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRevokeRequest;
import com.training.mapper.*;
import com.training.service.LearningRecordService;
import com.training.service.impl.CertificateServiceImpl;
import com.training.util.CertNoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @InjectMocks
    private CertificateServiceImpl certificateService;

    private Certificate validCert;

    @BeforeEach
    void setUp() {
        // Set @Value field that @InjectMocks cannot inject
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

    @Test
    @DisplayName("issue: should create certificate when completion is 100% and exam is passed")
    void issue_shouldSucceedWhenAllConditionsMet() {
        CertificateIssueRequest req = new CertificateIssueRequest();
        req.setStudentId(1L);
        req.setCourseId(10L);
        req.setTitle("Java Advanced Certificate");

        when(certificateMapper.selectOne(any())).thenReturn(null); // no existing cert
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
    @DisplayName("issue: should throw BusinessException when completion rate is below 100%")
    void issue_shouldThrowWhenCompletionRateInsufficient() {
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
    @DisplayName("issue: should throw BusinessException when exam has not been passed")
    void issue_shouldThrowWhenExamNotPassed() {
        CertificateIssueRequest req = new CertificateIssueRequest();
        req.setStudentId(1L);
        req.setCourseId(10L);

        when(certificateMapper.selectOne(any())).thenReturn(null);
        when(learningRecordService.getCompletionRate(1L, 10L)).thenReturn(100.0);
        when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList()); // no passing grades

        BusinessException ex = assertThrows(BusinessException.class,
                () -> certificateService.issue(req, 1L));
        assertTrue(ex.getMessage().contains("未通过"));

        verify(certificateMapper, never()).insert(any());
    }

    @Test
    @DisplayName("issue: should throw BusinessException when certificate already exists (duplicate)")
    void issue_shouldThrowOnDuplicateCertificate() {
        CertificateIssueRequest req = new CertificateIssueRequest();
        req.setStudentId(1L);
        req.setCourseId(10L);

        when(certificateMapper.selectOne(any())).thenReturn(validCert); // existing VALID cert

        BusinessException ex = assertThrows(BusinessException.class,
                () -> certificateService.issue(req, 1L));
        assertTrue(ex.getMessage().contains("已获得"));

        // Should not proceed to check completion rate or grades
        verify(learningRecordService, never()).getCompletionRate(anyLong(), anyLong());
        verify(gradeMapper, never()).selectList(any());
    }

    // ========================================================================
    // revoke tests
    // ========================================================================

    @Test
    @DisplayName("revoke: should update status to REVOKED and create revocation record")
    void revoke_shouldSucceed() {
        CertificateRevokeRequest req = new CertificateRevokeRequest();
        req.setReason("Academic dishonesty");

        when(certificateMapper.selectById(1L)).thenReturn(validCert);

        certificateService.revoke(1L, req, 99L);

        // Verify certificate status updated to REVOKED
        ArgumentCaptor<Certificate> certCaptor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateMapper).updateById(certCaptor.capture());
        Certificate updated = certCaptor.getValue();
        assertEquals("REVOKED", updated.getStatus());
        assertEquals("Academic dishonesty", updated.getRevokeReason());
        assertEquals(99L, updated.getRevokedBy());
        assertNotNull(updated.getRevokedAt());

        // Verify revocation record created
        ArgumentCaptor<CertificateRevocation> revCaptor = ArgumentCaptor.forClass(CertificateRevocation.class);
        verify(revocationMapper).insert(revCaptor.capture());
        assertEquals(1L, revCaptor.getValue().getCertificateId());
        assertEquals("Academic dishonesty", revCaptor.getValue().getReason());
        assertEquals(99L, revCaptor.getValue().getRevokedBy());
    }

    @Test
    @DisplayName("revoke: should throw BusinessException when certificate is already revoked")
    void revoke_shouldThrowWhenAlreadyRevoked() {
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

    // ========================================================================
    // verifyByCertNo tests
    // ========================================================================

    @Test
    @DisplayName("verifyByCertNo: should return valid=true for a valid certificate")
    void verifyByCertNo_shouldReturnValidForValidCert() {
        when(certificateMapper.selectOne(any())).thenReturn(validCert);

        Map<String, Object> result = certificateService.verifyByCertNo("CERT-10-20260610-0001");

        assertTrue((Boolean) result.get("valid"));
        assertEquals("证书有效", result.get("message"));
        assertEquals("CERT-10-20260610-0001", result.get("certNo"));
        assertEquals("Java Completion Certificate", result.get("title"));
        assertEquals(1L, result.get("studentId"));
        assertEquals(10L, result.get("courseId"));
    }

    @Test
    @DisplayName("verifyByCertNo: should return valid=false with revoke reason for revoked certificate")
    void verifyByCertNo_shouldReturnInvalidForRevokedCert() {
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
    @DisplayName("verifyByCertNo: should return valid=false for non-existent cert number")
    void verifyByCertNo_shouldReturnInvalidForNonExistent() {
        when(certificateMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = certificateService.verifyByCertNo("FAKE-CERT-999");

        assertFalse((Boolean) result.get("valid"));
        assertEquals("证书编号不存在", result.get("message"));
    }

    // ========================================================================
    // verifyByToken tests
    // ========================================================================

    @Test
    @DisplayName("verifyByToken: should return valid=false with expired flag for expired token")
    void verifyByToken_shouldReturnExpiredForExpiredToken() {
        validCert.setVerifyTokenExpiresAt(LocalDateTime.now().minusDays(1));

        when(certificateMapper.selectOne(any())).thenReturn(validCert);

        Map<String, Object> result = certificateService.verifyByToken("abc123token456");

        assertFalse((Boolean) result.get("valid"));
        assertTrue((Boolean) result.get("expired"));
        assertEquals("验证链接已过期，请联系管理员重新生成", result.get("message"));
        assertEquals(1L, result.get("certId"));
    }

    @Test
    @DisplayName("verifyByToken: should return valid=true for a valid unexpired token")
    void verifyByToken_shouldReturnValidForValidToken() {
        // Token expires in the future, cert is VALID
        validCert.setVerifyTokenExpiresAt(LocalDateTime.now().plusDays(5));

        when(certificateMapper.selectOne(any())).thenReturn(validCert);

        Map<String, Object> result = certificateService.verifyByToken("abc123token456");

        assertTrue((Boolean) result.get("valid"));
        assertEquals("证书有效", result.get("message"));
        assertEquals("CERT-10-20260610-0001", result.get("certNo"));
        assertNotNull(result.get("issueDate"));
    }

    @Test
    @DisplayName("verifyByToken: should return valid=false for non-existent token")
    void verifyByToken_shouldReturnInvalidForNonExistentToken() {
        when(certificateMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = certificateService.verifyByToken("nonexistent-token");

        assertFalse((Boolean) result.get("valid"));
        assertEquals("验证链接无效", result.get("message"));
    }
}
