package com.training.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.entity.Certificate;
import com.training.mapper.CertificateMapper;
import com.training.service.CertificateRenewalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateExpiryScheduler {

    private final CertificateMapper certificateMapper;
    private final CertificateRenewalService certificateRenewalService;

    /**
     * Daily at 2 AM: check certificates expiring within 90 days
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiringCertificates() {
        log.info("开始检查即将过期的证书...");

        LocalDate threshold = LocalDate.now().plusDays(90);
        List<Certificate> expiringCerts = certificateMapper.selectList(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getStatus, "VALID")
                        .isNotNull(Certificate::getExpiryDate)
                        .le(Certificate::getExpiryDate, threshold)
                        .gt(Certificate::getExpiryDate, LocalDate.now()));

        log.info("发现 {} 个即将过期的证书", expiringCerts.size());

        for (Certificate cert : expiringCerts) {
            try {
                certificateRenewalService.assessRenewal(cert.getId());
                log.info("证书续期评估完成: certId={}, certNo={}", cert.getId(), cert.getCertNo());
            } catch (Exception e) {
                log.error("证书续期评估失败: certId={}, certNo={}, error={}",
                        cert.getId(), cert.getCertNo(), e.getMessage(), e);
            }
        }

        log.info("证书过期检查完成");
    }
}
