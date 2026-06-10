package com.training.service;

import com.training.entity.CertificateRenewal;

import java.util.List;

public interface CertificateRenewalService {

    CertificateRenewal evaluateRenewal(Long certificateId, Long operatorId);

    void executeRenewal(Long renewalId, Long operatorId);

    List<CertificateRenewal> getHistory(Long certificateId);

    void scanExpiringCertificates();
}
