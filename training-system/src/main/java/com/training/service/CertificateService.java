package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.Certificate;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRevokeRequest;

import java.util.Map;

public interface CertificateService {
    Certificate issue(CertificateIssueRequest req, Long operatorId);
    IPage<Certificate> listByStudent(Long studentId, int page, int size);
    IPage<Certificate> listAll(int page, int size, String status);
    Certificate getById(Long id);
    void revoke(Long id, CertificateRevokeRequest req, Long operatorId);
    Map<String, Object> verifyByCertNo(String certNo);
    Map<String, Object> verifyByToken(String token);
    String regenerateVerifyToken(Long id);
}
