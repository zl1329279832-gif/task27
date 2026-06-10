package com.training.service;

import com.training.dto.request.CertificateIssueRequest;
import com.training.dto.request.CertificateRevokeRequest;
import com.training.entity.Certificate;
import com.training.entity.CertificateRevocation;
import com.training.entity.Course;
import com.training.entity.Exam;
import com.training.entity.Score;
import com.training.enums.CertificateStatus;
import com.training.exception.BusinessException;
import com.training.mapper.CertificateMapper;
import com.training.mapper.CertificateRevocationMapper;
import com.training.mapper.CourseMapper;
import com.training.mapper.CoursewareMapper;
import com.training.mapper.ExamMapper;
import com.training.mapper.LearningRecordMapper;
import com.training.mapper.ScoreMapper;
import com.training.security.JwtTokenProvider;
import com.training.service.impl.CertificateServiceImpl;
import com.training.util.CertNoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateMapper certificateMapper;

    @Mock
    private CertificateRevocationMapper certificateRevocationMapper;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CoursewareMapper coursewareMapper;

    @Mock
    private LearningRecordMapper learningRecordMapper;

    @Mock
    private ScoreMapper scoreMapper;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CertNoGenerator certNoGenerator;

    @InjectMocks
    private CertificateServiceImpl certificateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(certificateService, "certVerifyExpirationDays", 30);
    }

    @Test
    void testIssueCertificate_Success() {
        Long studentId = 1L;
        Long courseId = 1L;
        Long examId = 1L;
        Long issuedBy = 100L;

        CertificateIssueRequest request = new CertificateIssueRequest();
        request.setStudentId(studentId);
        request.setCourseId(courseId);
        request.setExamId(examId);

        when(certificateMapper.selectByStudentAndCourse(studentId, courseId)).thenReturn(null);

        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Java Programming");
        course.setCompletionThreshold(new BigDecimal("80"));
        when(courseMapper.selectById(courseId)).thenReturn(course);

        when(coursewareMapper.countByCourseId(courseId)).thenReturn(10);
        when(learningRecordMapper.countCompletedByStudentAndCourse(studentId, courseId)).thenReturn(9);

        Exam exam = new Exam();
        exam.setId(examId);
        exam.setPassScore(new BigDecimal("60"));
        when(examMapper.selectById(examId)).thenReturn(exam);

        Score bestScore = new Score();
        bestScore.setTotalScore(new BigDecimal("85"));
        when(scoreMapper.selectBestByStudentAndExam(studentId, examId)).thenReturn(bestScore);

        when(certNoGenerator.generate()).thenReturn("CERT-20240101-123456");

        doAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return 1;
        }).when(certificateMapper).insert(any(Certificate.class));

        when(jwtTokenProvider.generateCertVerifyToken(1L, 30)).thenReturn("verify-jwt-token");
        when(certificateMapper.updateVerifyToken(eq(1L), eq("verify-jwt-token"), any(LocalDateTime.class))).thenReturn(1);

        Certificate result = certificateService.issueCertificate(request, issuedBy);

        assertNotNull(result);
        assertEquals("CERT-20240101-123456", result.getCertNo());
        assertEquals("verify-jwt-token", result.getVerifyToken());
        assertEquals(studentId, result.getStudentId());
        assertEquals(courseId, result.getCourseId());
        assertEquals(CertificateStatus.VALID.name(), result.getStatus());

        verify(certificateMapper).insert(any(Certificate.class));
        verify(certificateMapper).updateVerifyToken(eq(1L), eq("verify-jwt-token"), any(LocalDateTime.class));
    }

    @Test
    void testIssueCertificate_AlreadyExists() {
        Long studentId = 1L;
        Long courseId = 1L;
        Long issuedBy = 100L;

        CertificateIssueRequest request = new CertificateIssueRequest();
        request.setStudentId(studentId);
        request.setCourseId(courseId);

        Certificate existing = new Certificate();
        existing.setId(1L);
        existing.setStudentId(studentId);
        existing.setCourseId(courseId);
        when(certificateMapper.selectByStudentAndCourse(studentId, courseId)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> certificateService.issueCertificate(request, issuedBy));

        assertTrue(exception.getMessage().contains("已获得此课程的证书"));
    }

    @Test
    void testIssueCertificate_CompletionRateInsufficient() {
        Long studentId = 1L;
        Long courseId = 1L;
        Long issuedBy = 100L;

        CertificateIssueRequest request = new CertificateIssueRequest();
        request.setStudentId(studentId);
        request.setCourseId(courseId);

        when(certificateMapper.selectByStudentAndCourse(studentId, courseId)).thenReturn(null);

        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Java Programming");
        course.setCompletionThreshold(new BigDecimal("80"));
        when(courseMapper.selectById(courseId)).thenReturn(course);

        when(coursewareMapper.countByCourseId(courseId)).thenReturn(10);
        when(learningRecordMapper.countCompletedByStudentAndCourse(studentId, courseId)).thenReturn(5);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> certificateService.issueCertificate(request, issuedBy));

        assertTrue(exception.getMessage().contains("完成率不满足"));
    }

    @Test
    void testIssueCertificate_ExamNotPassed() {
        Long studentId = 1L;
        Long courseId = 1L;
        Long examId = 1L;
        Long issuedBy = 100L;

        CertificateIssueRequest request = new CertificateIssueRequest();
        request.setStudentId(studentId);
        request.setCourseId(courseId);
        request.setExamId(examId);

        when(certificateMapper.selectByStudentAndCourse(studentId, courseId)).thenReturn(null);

        Course course = new Course();
        course.setId(courseId);
        course.setTitle("Java Programming");
        course.setCompletionThreshold(new BigDecimal("80"));
        when(courseMapper.selectById(courseId)).thenReturn(course);

        when(coursewareMapper.countByCourseId(courseId)).thenReturn(10);
        when(learningRecordMapper.countCompletedByStudentAndCourse(studentId, courseId)).thenReturn(9);

        Exam exam = new Exam();
        exam.setId(examId);
        exam.setPassScore(new BigDecimal("60"));
        when(examMapper.selectById(examId)).thenReturn(exam);

        Score bestScore = new Score();
        bestScore.setTotalScore(new BigDecimal("50"));
        when(scoreMapper.selectBestByStudentAndExam(studentId, examId)).thenReturn(bestScore);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> certificateService.issueCertificate(request, issuedBy));

        assertTrue(exception.getMessage().contains("成绩未达到"));
    }

    @Test
    void testVerifyByCertNo_Valid() {
        String certNo = "CERT-20240101-123456";

        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setCertNo(certNo);
        certificate.setStudentId(1L);
        certificate.setCourseId(1L);
        certificate.setTitle("Java Programming Certificate");
        certificate.setStatus(CertificateStatus.VALID.name());
        certificate.setIssuedAt(LocalDateTime.now());

        when(certificateMapper.selectByCertNo(certNo)).thenReturn(certificate);

        Map<String, Object> result = certificateService.verifyByCertNo(certNo);

        assertNotNull(result);
        assertEquals(true, result.get("valid"));
        assertNotNull(result.get("certificate"));
    }

    @Test
    void testVerifyByCertNo_Revoked() {
        String certNo = "CERT-20240101-123456";

        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setCertNo(certNo);
        certificate.setStatus(CertificateStatus.REVOKED.name());

        CertificateRevocation revocation = new CertificateRevocation();
        revocation.setCertificateId(1L);
        revocation.setRevokedAt(LocalDateTime.now());
        revocation.setReason("Cheating detected");

        when(certificateMapper.selectByCertNo(certNo)).thenReturn(certificate);
        when(certificateRevocationMapper.selectByCertificateId(1L)).thenReturn(revocation);

        Map<String, Object> result = certificateService.verifyByCertNo(certNo);

        assertNotNull(result);
        assertEquals(false, result.get("valid"));
        assertTrue(result.get("message").toString().contains("撤销"));
    }

    @Test
    void testVerifyByCertNo_NotFound() {
        String certNo = "CERT-NONEXISTENT";

        when(certificateMapper.selectByCertNo(certNo)).thenReturn(null);

        Map<String, Object> result = certificateService.verifyByCertNo(certNo);

        assertNotNull(result);
        assertEquals(false, result.get("valid"));
    }

    @Test
    void testVerifyByToken_ValidToken() {
        String token = "valid-jwt-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getCertificateId(token)).thenReturn(1L);

        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setCertNo("CERT-20240101-123456");
        certificate.setStudentId(1L);
        certificate.setCourseId(1L);
        certificate.setTitle("Java Programming Certificate");
        certificate.setStatus(CertificateStatus.VALID.name());
        certificate.setIssuedAt(LocalDateTime.now());

        when(certificateMapper.selectById(1L)).thenReturn(certificate);

        Map<String, Object> result = certificateService.verifyByToken(token);

        assertNotNull(result);
        assertEquals(true, result.get("valid"));
        assertNotNull(result.get("certificate"));
    }

    @Test
    void testVerifyByToken_ExpiredToken() {
        String token = "expired-jwt-token";

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        Map<String, Object> result = certificateService.verifyByToken(token);

        assertNotNull(result);
        assertEquals(false, result.get("valid"));
        assertTrue(result.get("message").toString().contains("过期"));
    }

    @Test
    void testRevokeCertificate_Success() {
        Long certificateId = 1L;
        Long revokedBy = 100L;

        CertificateRevokeRequest request = new CertificateRevokeRequest();
        request.setCertificateId(certificateId);
        request.setReason("Academic dishonesty");

        Certificate certificate = new Certificate();
        certificate.setId(certificateId);
        certificate.setStatus(CertificateStatus.VALID.name());

        when(certificateMapper.selectById(certificateId)).thenReturn(certificate);
        when(certificateMapper.updateStatus(certificateId, CertificateStatus.REVOKED.name())).thenReturn(1);
        when(certificateRevocationMapper.insert(any(CertificateRevocation.class))).thenReturn(1);

        certificateService.revokeCertificate(request, revokedBy);

        verify(certificateMapper).updateStatus(certificateId, CertificateStatus.REVOKED.name());

        ArgumentCaptor<CertificateRevocation> captor = ArgumentCaptor.forClass(CertificateRevocation.class);
        verify(certificateRevocationMapper).insert(captor.capture());
        CertificateRevocation revocation = captor.getValue();
        assertEquals(certificateId, revocation.getCertificateId());
        assertEquals("Academic dishonesty", revocation.getReason());
        assertEquals(revokedBy, revocation.getRevokedBy());
        assertNotNull(revocation.getRevokedAt());
    }

    @Test
    void testRevokeCertificate_AlreadyRevoked() {
        Long certificateId = 1L;
        Long revokedBy = 100L;

        CertificateRevokeRequest request = new CertificateRevokeRequest();
        request.setCertificateId(certificateId);
        request.setReason("Some reason");

        Certificate certificate = new Certificate();
        certificate.setId(certificateId);
        certificate.setStatus(CertificateStatus.REVOKED.name());

        when(certificateMapper.selectById(certificateId)).thenReturn(certificate);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> certificateService.revokeCertificate(request, revokedBy));

        assertTrue(exception.getMessage().contains("已经被撤销"));
    }
}
