package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.CertificateRenewal;
import com.training.entity.MakeupExam;
import com.training.mapper.CertificateRenewalMapper;
import com.training.mapper.MakeupExamMapper;
import com.training.service.AuditLogService;
import com.training.service.MakeupExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MakeupExamServiceImpl implements MakeupExamService {

    private final MakeupExamMapper makeupExamMapper;
    private final CertificateRenewalMapper certificateRenewalMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public MakeupExam create(MakeupExam makeupExam) {
        makeupExam.setStatus("PENDING");
        makeupExam.setAttemptsUsed(0);
        makeupExamMapper.insert(makeupExam);
        return makeupExam;
    }

    @Override
    public IPage<MakeupExam> listByStudent(Long studentId, int page, int size) {
        return makeupExamMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MakeupExam>()
                        .eq(MakeupExam::getStudentId, studentId)
                        .orderByDesc(MakeupExam::getCreatedAt));
    }

    @Override
    @Transactional
    public void startMakeupExam(Long makeupExamId) {
        MakeupExam exam = makeupExamMapper.selectById(makeupExamId);
        if (exam == null) throw new BusinessException("补考任务不存在");
        if (!"PENDING".equals(exam.getStatus())) throw new BusinessException("补考任务状态不允许开始");
        if (exam.getAttemptsUsed() >= exam.getMaxAttempts())
            throw new BusinessException("已达到最大补考次数: " + exam.getMaxAttempts());
        if (exam.getDeadline() != null && LocalDateTime.now().isAfter(exam.getDeadline()))
            throw new BusinessException("补考已过期");

        exam.setStatus("IN_PROGRESS");
        exam.setAttemptsUsed(exam.getAttemptsUsed() + 1);
        makeupExamMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void recordMakeupExamResult(Long makeupExamId, double score) {
        MakeupExam exam = makeupExamMapper.selectById(makeupExamId);
        if (exam == null) throw new BusinessException("补考任务不存在");
        if (!"IN_PROGRESS".equals(exam.getStatus())) throw new BusinessException("补考未在进行中");

        exam.setAchievedScore(BigDecimal.valueOf(score));

        if (exam.getRequiredScore() != null
                && BigDecimal.valueOf(score).compareTo(exam.getRequiredScore()) >= 0) {
            exam.setStatus("PASSED");
            exam.setCompletedAt(LocalDateTime.now());
            makeupExamMapper.updateById(exam);

            // Update linked CertificateRenewal if exists
            CertificateRenewal renewal = certificateRenewalMapper.selectOne(
                    new LambdaQueryWrapper<CertificateRenewal>()
                            .eq(CertificateRenewal::getMakeupExamId, makeupExamId)
                            .eq(CertificateRenewal::getStatus, "IN_PROGRESS"));
            if (renewal != null) {
                renewal.setStatus("COMPLETED");
                renewal.setProcessedAt(LocalDateTime.now());
                certificateRenewalMapper.updateById(renewal);
            }

            auditLogService.log("MAKEUP_EXAM_PASSED", "MAKEUP_EXAM", makeupExamId,
                    exam.getStudentId(), "STUDENT",
                    Map.of("score", score, "examId", exam.getExamId()));
        } else {
            exam.setStatus("FAILED");
            makeupExamMapper.updateById(exam);

            auditLogService.log("MAKEUP_EXAM_FAILED", "MAKEUP_EXAM", makeupExamId,
                    exam.getStudentId(), "STUDENT",
                    Map.of("score", score, "required", exam.getRequiredScore()));
        }
    }
}
