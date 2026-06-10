package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.CertificateRenewal;
import com.training.entity.CertificateRenewalRule;
import com.training.entity.dto.CertificateRenewalRuleRequest;
import com.training.entity.dto.RenewalAssessmentResult;

import java.util.List;

public interface CertificateRenewalService {

    RenewalAssessmentResult assessRenewal(Long certificateId);

    CertificateRenewal initiateRenewal(Long certificateId, Long operatorId);

    void completeRenewal(Long renewalId, Long operatorId);

    void rejectRenewal(Long renewalId, String reason, Long operatorId);

    IPage<CertificateRenewal> listByStudent(Long studentId, int page, int size);

    IPage<CertificateRenewal> listAll(int page, int size);

    CertificateRenewal getById(Long id);

    CertificateRenewalRule createRule(CertificateRenewalRuleRequest req);

    CertificateRenewalRule updateRule(Long id, CertificateRenewalRuleRequest req);

    void deleteRule(Long id);

    List<CertificateRenewalRule> listRules(Long courseId);
}
