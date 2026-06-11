package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.KnowledgePointMasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePointMasteryServiceImpl implements KnowledgePointMasteryService {

    private final KnowledgePointMasteryMapper masteryMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final QuestionKnowledgePointMapper questionKnowledgePointMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final AuditLogService auditLogService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void updateMasteryFromExam(Long studentId, Long courseId, Long answerSheetId) {
        // Distributed lock: prevent concurrent mastery updates from racing on read-modify-write
        String lockKey = "mastery:update:" + studentId + ":" + courseId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("知识点掌握度正在更新中，请稍后重试");
        }

        try {
            doUpdateMasteryFromExam(studentId, courseId, answerSheetId);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void doUpdateMasteryFromExam(Long studentId, Long courseId, Long answerSheetId) {
        List<AnswerDetail> details = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>()
                        .eq(AnswerDetail::getAnswerSheetId, answerSheetId));

        if (details.isEmpty()) return;

        // Build questionId -> AnswerDetail map
        Map<Long, AnswerDetail> detailMap = details.stream()
                .collect(Collectors.toMap(AnswerDetail::getQuestionId, d -> d, (a, b) -> a));

        // For each question, find associated knowledge points
        // kpId -> {total, correct}
        Map<Long, int[]> kpStats = new HashMap<>();

        for (Map.Entry<Long, AnswerDetail> entry : detailMap.entrySet()) {
            Long questionId = entry.getKey();
            AnswerDetail detail = entry.getValue();

            List<QuestionKnowledgePoint> qkps = questionKnowledgePointMapper.selectList(
                    new LambdaQueryWrapper<QuestionKnowledgePoint>()
                            .eq(QuestionKnowledgePoint::getQuestionId, questionId));

            for (QuestionKnowledgePoint qkp : qkps) {
                Long kpId = qkp.getKnowledgePointId();
                int[] stats = kpStats.computeIfAbsent(kpId, k -> new int[]{0, 0});
                stats[0]++; // total
                if (detail.getIsCorrect() != null && detail.getIsCorrect() == 1) {
                    stats[1]++; // correct
                }
            }
        }

        // Upsert mastery records
        for (Map.Entry<Long, int[]> entry : kpStats.entrySet()) {
            Long kpId = entry.getKey();
            int[] stats = entry.getValue();
            int total = stats[0];
            int correct = stats[1];

            KnowledgePointMastery existing = masteryMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgePointMastery>()
                            .eq(KnowledgePointMastery::getStudentId, studentId)
                            .eq(KnowledgePointMastery::getKnowledgePointId, kpId));

            if (existing != null) {
                // Incremental update — merge stats
                int newTotal = existing.getTotalQuestions() + total;
                int newCorrect = existing.getCorrectCount() + correct;
                BigDecimal mastery = BigDecimal.valueOf(newCorrect)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP);

                existing.setTotalQuestions(newTotal);
                existing.setCorrectCount(newCorrect);
                existing.setMasteryLevel(mastery);
                existing.setExamAttempts(existing.getExamAttempts() + 1);
                existing.setStatus(computeStatus(mastery));
                existing.setLastAssessedAt(LocalDateTime.now());
                masteryMapper.updateById(existing);
            } else {
                BigDecimal mastery = BigDecimal.valueOf(correct)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

                KnowledgePointMastery masteryRecord = KnowledgePointMastery.builder()
                        .studentId(studentId)
                        .knowledgePointId(kpId)
                        .courseId(courseId)
                        .masteryLevel(mastery)
                        .totalQuestions(total)
                        .correctCount(correct)
                        .examAttempts(1)
                        .status(computeStatus(mastery))
                        .lastAssessedAt(LocalDateTime.now())
                        .build();
                masteryMapper.insert(masteryRecord);
            }
        }

        auditLogService.log("MASTERY_UPDATE", "ANSWER_SHEET", answerSheetId,
                studentId, "STUDENT", Map.of("courseId", courseId, "kpCount", kpStats.size()));
    }

    @Override
    @Transactional
    public void recalculateMastery(Long studentId, Long courseId) {
        // Delete existing mastery records for this student+course
        masteryMapper.delete(
                new LambdaQueryWrapper<KnowledgePointMastery>()
                        .eq(KnowledgePointMastery::getStudentId, studentId)
                        .eq(KnowledgePointMastery::getCourseId, courseId));

        // Get all answer sheets for this student in this course
        List<AnswerSheet> sheets = answerSheetMapper.selectList(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getStudentId, studentId)
                        .ne(AnswerSheet::getStatus, "IN_PROGRESS"));

        // Filter by course via exam
        for (AnswerSheet sheet : sheets) {
            // Re-process each sheet's details (without incrementing exam attempts multiple times)
            List<AnswerDetail> details = answerDetailMapper.selectList(
                    new LambdaQueryWrapper<AnswerDetail>()
                            .eq(AnswerDetail::getAnswerSheetId, sheet.getId()));

            Map<Long, int[]> kpStats = new HashMap<>();
            for (AnswerDetail detail : details) {
                List<QuestionKnowledgePoint> qkps = questionKnowledgePointMapper.selectList(
                        new LambdaQueryWrapper<QuestionKnowledgePoint>()
                                .eq(QuestionKnowledgePoint::getQuestionId, detail.getQuestionId()));
                for (QuestionKnowledgePoint qkp : qkps) {
                    Long kpId = qkp.getKnowledgePointId();
                    int[] stats = kpStats.computeIfAbsent(kpId, k -> new int[]{0, 0});
                    stats[0]++;
                    if (detail.getIsCorrect() != null && detail.getIsCorrect() == 1) {
                        stats[1]++;
                    }
                }
            }

            for (Map.Entry<Long, int[]> entry : kpStats.entrySet()) {
                Long kpId = entry.getKey();
                int[] stats = entry.getValue();

                KnowledgePointMastery existing = masteryMapper.selectOne(
                        new LambdaQueryWrapper<KnowledgePointMastery>()
                                .eq(KnowledgePointMastery::getStudentId, studentId)
                                .eq(KnowledgePointMastery::getKnowledgePointId, kpId));

                if (existing != null) {
                    int newTotal = existing.getTotalQuestions() + stats[0];
                    int newCorrect = existing.getCorrectCount() + stats[1];
                    BigDecimal mastery = BigDecimal.valueOf(newCorrect)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP);
                    existing.setTotalQuestions(newTotal);
                    existing.setCorrectCount(newCorrect);
                    existing.setMasteryLevel(mastery);
                    existing.setExamAttempts(existing.getExamAttempts() + 1);
                    existing.setStatus(computeStatus(mastery));
                    existing.setLastAssessedAt(LocalDateTime.now());
                    masteryMapper.updateById(existing);
                } else {
                    BigDecimal mastery = BigDecimal.valueOf(stats[1])
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(stats[0]), 2, RoundingMode.HALF_UP);
                    KnowledgePointMastery record = KnowledgePointMastery.builder()
                            .studentId(studentId)
                            .knowledgePointId(kpId)
                            .courseId(courseId)
                            .masteryLevel(mastery)
                            .totalQuestions(stats[0])
                            .correctCount(stats[1])
                            .examAttempts(1)
                            .status(computeStatus(mastery))
                            .lastAssessedAt(LocalDateTime.now())
                            .build();
                    masteryMapper.insert(record);
                }
            }
        }
    }

    @Override
    public List<KnowledgePointMastery> getUnmasteredPoints(Long studentId, Long courseId, double threshold) {
        return masteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgePointMastery>()
                        .eq(KnowledgePointMastery::getStudentId, studentId)
                        .eq(KnowledgePointMastery::getCourseId, courseId)
                        .lt(KnowledgePointMastery::getMasteryLevel, BigDecimal.valueOf(threshold))
                        .orderByAsc(KnowledgePointMastery::getMasteryLevel));
    }

    @Override
    public KnowledgePointMastery getMastery(Long studentId, Long knowledgePointId) {
        return masteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgePointMastery>()
                        .eq(KnowledgePointMastery::getStudentId, studentId)
                        .eq(KnowledgePointMastery::getKnowledgePointId, knowledgePointId));
    }

    private String computeStatus(BigDecimal masteryLevel) {
        if (masteryLevel.compareTo(BigDecimal.valueOf(80)) >= 0) return "MASTERED";
        if (masteryLevel.compareTo(BigDecimal.valueOf(60)) >= 0) return "PARTIAL";
        return "UNMASTERED";
    }
}
