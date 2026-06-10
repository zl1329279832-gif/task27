package com.training.service;

import com.training.dto.request.CertificateIssueRequest;
import com.training.dto.request.CertificateRevokeRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Certificate;

import java.util.List;
import java.util.Map;

public interface CertificateService {

    Certificate issueCertificate(CertificateIssueRequest request, Long issuedBy);

    Certificate getCertificateById(Long id);

    List<Certificate> getCertificatesByStudent(Long studentId);

    PageResult<Certificate> listCertificates(String keyword, String status, int page, int size);

    Map<String, Object> verifyByCertNo(String certNo);

    Map<String, Object> verifyByToken(String token);

    void revokeCertificate(CertificateRevokeRequest request, Long revokedBy);

    String regenerateVerifyToken(Long certificateId);
}
