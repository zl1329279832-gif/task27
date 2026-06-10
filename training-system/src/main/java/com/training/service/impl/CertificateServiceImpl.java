package com.training.service.impl;

import com.github.pagehelper.PageHelper;
import com.training.dto.request.CertificateIssueRequest;
import com.training.dto.request.CertificateRevokeRequest;
import com.training.dto.response.PageResult;
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
import com.training.service.CertificateService;
import com.training.util.CertNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateMapper certificateMapper;
    private final CertificateRevocationMapper certificateRevocationMapper;
    private final CourseMapper courseMapper;
    private final CoursewareMapper coursewareMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ScoreMapper scoreMapper;
    private final ExamMapper examMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final CertNoGenerator certNoGenerator;

    @Value("${jwt.cert-verify-expiration-days}")
    private int certVerifyExpirationDays;

    @Override
    public Certificate issueCertificate(CertificateIssueRequest request, Long issuedBy) {
        log.info("颁发证书, studentId: {}, courseId: {}, issuedBy: {}",
                request.getStudentId(), request.getCourseId(), issuedBy);

        // 1. Check not already issued
        Certificate existing = certificateMapper.selectByStudentAndCourse(
                request.getStudentId(), request.getCourseId());
        if (existing != null) {
            throw new BusinessException("该学员已获得此课程的证书");
        }

        // 2. Get course and check exists
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // 3. Check completion rate
        int totalCoursewares = coursewareMapper.countByCourseId(request.getCourseId());
        int completedCoursewares = learningRecordMapper.countCompletedByStudentAndCourse(
                request.getStudentId(), request.getCourseId());
        double completionRate = (totalCoursewares == 0) ? 0 : completedCoursewares * 100.0 / totalCoursewares;

        if (course.getCompletionThreshold() != null
                && completionRate < course.getCompletionThreshold().doubleValue()) {
            throw new BusinessException("课程完成率不满足要求，当前: " + String.format("%.1f", completionRate)
                    + "%，要求: " + course.getCompletionThreshold() + "%");
        }

        // 4. Check exam score if examId provided
        if (request.getExamId() != null) {
            Exam exam = examMapper.selectById(request.getExamId());
            if (exam == null) {
                throw new BusinessException("考试不存在");
            }
            Score bestScore = scoreMapper.selectBestByStudentAndExam(request.getStudentId(), request.getExamId());
            if (bestScore == null) {
                throw new BusinessException("该学员未参加此考试");
            }
            if (bestScore.getTotalScore().compareTo(exam.getPassScore()) < 0) {
                throw new BusinessException("考试成绩未达到及格分数");
            }
        }

        // 5. Generate unique cert no with retry
        Certificate certificate = null;
        for (int i = 0; i < 3; i++) {
            String certNo = certNoGenerator.generate();
            try {
                // 6. Build certificate
                LocalDateTime now = LocalDateTime.now();
                String title = request.getTitle() != null ? request.getTitle() : course.getTitle() + " 结业证书";

                certificate = Certificate.builder()
                        .certNo(certNo)
                        .studentId(request.getStudentId())
                        .courseId(request.getCourseId())
                        .examId(request.getExamId())
                        .title(title)
                        .description(request.getDescription())
                        .issuedAt(now)
                        .status(CertificateStatus.VALID.name())
                        .build();

                // 7. Generate verify token
                certificateMapper.insert(certificate);

                String verifyToken = jwtTokenProvider.generateCertVerifyToken(
                        certificate.getId(), certVerifyExpirationDays);
                LocalDateTime tokenExpiresAt = now.plusDays(certVerifyExpirationDays);

                // 8. Update verify token
                certificateMapper.updateVerifyToken(certificate.getId(), verifyToken, tokenExpiresAt);
                certificate.setVerifyToken(verifyToken);
                certificate.setVerifyTokenExpiresAt(tokenExpiresAt);

                log.info("证书颁发成功, certificateId: {}, certNo: {}", certificate.getId(), certNo);
                return certificate;
            } catch (DuplicateKeyException e) {
                log.warn("证书编号重复, certNo: {}, 重试第{}次", certNo, i + 1);
                if (i == 2) {
                    throw new BusinessException("证书编号生成失败，请重试");
                }
            }
        }

        throw new BusinessException("证书编号生成失败，请重试");
    }

    @Override
    public Certificate getCertificateById(Long id) {
        log.info("查询证书详情, id: {}", id);
        Certificate certificate = certificateMapper.selectById(id);
        if (certificate == null) {
            throw new BusinessException("证书不存在");
        }
        return certificate;
    }

    @Override
    public List<Certificate> getCertificatesByStudent(Long studentId) {
        log.info("查询学员证书列表, studentId: {}", studentId);
        return certificateMapper.selectByStudentId(studentId);
    }

    @Override
    public PageResult<Certificate> listCertificates(String keyword, String status, int page, int size) {
        log.info("查询证书列表, keyword: {}, status: {}, page: {}, size: {}",
                keyword, status, page, size);
        PageHelper.startPage(page, size);
        List<Certificate> list = certificateMapper.selectList(keyword, status);
        return PageResult.of(list);
    }

    @Override
    public Map<String, Object> verifyByCertNo(String certNo) {
        log.info("通过证书编号验证, certNo: {}", certNo);
        Map<String, Object> result = new HashMap<>();

        // 1. Query certificate by certNo
        Certificate certificate = certificateMapper.selectByCertNo(certNo);
        if (certificate == null) {
            result.put("valid", false);
            result.put("message", "证书不存在");
            return result;
        }

        // 2. Check revocation status
        if (CertificateStatus.REVOKED.name().equals(certificate.getStatus())) {
            result.put("valid", false);
            result.put("message", "证书已被撤销");
            CertificateRevocation revocation = certificateRevocationMapper.selectByCertificateId(certificate.getId());
            if (revocation != null) {
                result.put("revokedAt", revocation.getRevokedAt());
            }
            return result;
        }

        // 3. Valid certificate
        result.put("valid", true);
        result.put("certificate", buildCertificateDetail(certificate));
        return result;
    }

    @Override
    public Map<String, Object> verifyByToken(String token) {
        log.info("通过验证链接验证证书");
        Map<String, Object> result = new HashMap<>();

        // 1. Validate JWT token
        if (!jwtTokenProvider.validateToken(token)) {
            result.put("valid", false);
            result.put("message", "验证链接无效或已过期");
            return result;
        }

        // 2. Parse certificateId from token
        Long certificateId;
        try {
            certificateId = jwtTokenProvider.getCertificateId(token);
        } catch (Exception e) {
            log.error("解析证书验证令牌失败", e);
            result.put("valid", false);
            result.put("message", "验证链接无效或已过期");
            return result;
        }

        // 3. Get certificate
        Certificate certificate = certificateMapper.selectById(certificateId);
        if (certificate == null) {
            result.put("valid", false);
            result.put("message", "证书不存在");
            return result;
        }

        // 4. Check revocation status
        if (CertificateStatus.REVOKED.name().equals(certificate.getStatus())) {
            result.put("valid", false);
            result.put("message", "证书已被撤销");
            return result;
        }

        // 5. Valid certificate
        result.put("valid", true);
        result.put("certificate", buildCertificateDetail(certificate));
        return result;
    }

    @Override
    public void revokeCertificate(CertificateRevokeRequest request, Long revokedBy) {
        log.info("撤销证书, certificateId: {}, revokedBy: {}", request.getCertificateId(), revokedBy);

        // 1. Get certificate
        Certificate certificate = certificateMapper.selectById(request.getCertificateId());
        if (certificate == null) {
            throw new BusinessException("证书不存在");
        }

        // 2. Check if already revoked
        if (CertificateStatus.REVOKED.name().equals(certificate.getStatus())) {
            throw new BusinessException("证书已经被撤销");
        }

        // 3. Update certificate status to REVOKED
        certificateMapper.updateStatus(certificate.getId(), CertificateStatus.REVOKED.name());

        // 4. Create revocation record
        CertificateRevocation revocation = CertificateRevocation.builder()
                .certificateId(certificate.getId())
                .reason(request.getReason())
                .revokedBy(revokedBy)
                .revokedAt(LocalDateTime.now())
                .build();
        certificateRevocationMapper.insert(revocation);

        log.info("证书撤销成功, certificateId: {}", certificate.getId());
    }

    @Override
    public String regenerateVerifyToken(Long certificateId) {
        log.info("重新生成验证令牌, certificateId: {}", certificateId);

        // 1. Get certificate and check status
        Certificate certificate = certificateMapper.selectById(certificateId);
        if (certificate == null) {
            throw new BusinessException("证书不存在");
        }
        if (!CertificateStatus.VALID.name().equals(certificate.getStatus())) {
            throw new BusinessException("只有有效状态的证书才能重新生成验证令牌");
        }

        // 2. Generate new JWT verify token
        String newToken = jwtTokenProvider.generateCertVerifyToken(certificateId, certVerifyExpirationDays);
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusDays(certVerifyExpirationDays);

        // 3. Update verify token in DB
        certificateMapper.updateVerifyToken(certificateId, newToken, tokenExpiresAt);

        log.info("验证令牌重新生成成功, certificateId: {}", certificateId);
        return newToken;
    }

    /**
     * Build certificate detail map for verification responses.
     */
    private Map<String, Object> buildCertificateDetail(Certificate certificate) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", certificate.getId());
        detail.put("certNo", certificate.getCertNo());
        detail.put("studentId", certificate.getStudentId());
        detail.put("courseId", certificate.getCourseId());
        detail.put("title", certificate.getTitle());
        detail.put("description", certificate.getDescription());
        detail.put("issuedAt", certificate.getIssuedAt());
        detail.put("status", certificate.getStatus());
        return detail;
    }
}
