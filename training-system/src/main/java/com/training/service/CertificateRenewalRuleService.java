package com.training.service;

import com.training.entity.CertificateRenewalRule;
import com.training.entity.dto.RenewalRuleRequest;

public interface CertificateRenewalRuleService {

    CertificateRenewalRule create(RenewalRuleRequest req);

    CertificateRenewalRule update(Long id, RenewalRuleRequest req);

    CertificateRenewalRule getByCourse(Long courseId);

    void delete(Long id);
}
