package com.training.scheduler;

import com.training.service.CertificateRenewalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateRenewalScheduler {

    private final CertificateRenewalService certificateRenewalService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void scanExpiringCertificates() {
        log.info("Scanning for certificates nearing expiry...");
        try {
            certificateRenewalService.scanExpiringCertificates();
            log.info("Certificate renewal scan completed.");
        } catch (Exception e) {
            log.error("Certificate renewal scan failed: {}", e.getMessage(), e);
        }
    }
}
