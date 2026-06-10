package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.CertificateRenewalRule;
import com.training.entity.dto.RenewalRuleRequest;
import com.training.mapper.CertificateRenewalRuleMapper;
import com.training.service.CertificateRenewalRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificateRenewalRuleServiceImpl implements CertificateRenewalRuleService {

    private final CertificateRenewalRuleMapper renewalRuleMapper;

    @Override
    @Transactional
    public CertificateRenewalRule create(RenewalRuleRequest req) {
        CertificateRenewalRule existing = renewalRuleMapper.selectOne(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getCourseId, req.getCourseId()));
        if (existing != null) {
            throw new BusinessException("该课程已存在续期规则");
        }

        CertificateRenewalRule rule = CertificateRenewalRule.builder()
                .courseId(req.getCourseId())
                .renewalPeriodDays(req.getRenewalPeriodDays())
                .advanceNoticeDays(req.getAdvanceNoticeDays() != null ? req.getAdvanceNoticeDays() : 30)
                .requireExamPass(req.getRequireExamPass() != null && req.getRequireExamPass() ? 1 : 0)
                .minExamScore(req.getMinExamScore())
                .versionChangePolicy(req.getVersionChangePolicy() != null ? req.getVersionChangePolicy() : "RELEARN")
                .roleExemptions(req.getRoleExemptions())
                .enabled(req.getEnabled() != null && req.getEnabled() ? 1 : 0)
                .build();
        renewalRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public CertificateRenewalRule update(Long id, RenewalRuleRequest req) {
        CertificateRenewalRule rule = renewalRuleMapper.selectById(id);
        if (rule == null) throw new BusinessException("续期规则不存在");

        if (req.getRenewalPeriodDays() != null) rule.setRenewalPeriodDays(req.getRenewalPeriodDays());
        if (req.getAdvanceNoticeDays() != null) rule.setAdvanceNoticeDays(req.getAdvanceNoticeDays());
        if (req.getRequireExamPass() != null) rule.setRequireExamPass(req.getRequireExamPass() ? 1 : 0);
        if (req.getMinExamScore() != null) rule.setMinExamScore(req.getMinExamScore());
        if (req.getVersionChangePolicy() != null) rule.setVersionChangePolicy(req.getVersionChangePolicy());
        if (req.getRoleExemptions() != null) rule.setRoleExemptions(req.getRoleExemptions());
        if (req.getEnabled() != null) rule.setEnabled(req.getEnabled() ? 1 : 0);

        renewalRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    public CertificateRenewalRule getByCourse(Long courseId) {
        return renewalRuleMapper.selectOne(
                new LambdaQueryWrapper<CertificateRenewalRule>()
                        .eq(CertificateRenewalRule::getCourseId, courseId));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CertificateRenewalRule rule = renewalRuleMapper.selectById(id);
        if (rule == null) throw new BusinessException("续期规则不存在");
        renewalRuleMapper.deleteById(id);
    }
}
