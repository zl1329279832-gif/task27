package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.KnowledgePointMasteryDTO;
import com.training.entity.dto.KnowledgePointRequest;
import com.training.mapper.*;
import com.training.service.KnowledgePointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionKnowledgePointMapper questionKnowledgePointMapper;
    private final KnowledgePointMasteryMapper knowledgePointMasteryMapper;

    @Override
    @Transactional
    public KnowledgePoint create(KnowledgePointRequest req) {
        KnowledgePoint kp = KnowledgePoint.builder()
                .courseId(req.getCourseId())
                .chapterId(req.getChapterId())
                .name(req.getName())
                .description(req.getDescription())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .parentKpId(req.getParentKpId())
                .build();
        knowledgePointMapper.insert(kp);
        return kp;
    }

    @Override
    @Transactional
    public KnowledgePoint update(Long id, KnowledgePointRequest req) {
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        if (kp == null) throw new BusinessException("知识点不存在");

        kp.setName(req.getName() != null ? req.getName() : kp.getName());
        kp.setDescription(req.getDescription() != null ? req.getDescription() : kp.getDescription());
        kp.setChapterId(req.getChapterId() != null ? req.getChapterId() : kp.getChapterId());
        kp.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : kp.getSortOrder());
        knowledgePointMapper.updateById(kp);
        return kp;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        if (kp == null) throw new BusinessException("知识点不存在");
        questionKnowledgePointMapper.delete(
                new LambdaQueryWrapper<QuestionKnowledgePoint>()
                        .eq(QuestionKnowledgePoint::getKnowledgePointId, id));
        knowledgePointMapper.deleteById(id);
    }

    @Override
    public List<KnowledgePoint> listByCourse(Long courseId) {
        return knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getCourseId, courseId)
                        .orderByAsc(KnowledgePoint::getSortOrder));
    }

    @Override
    public List<KnowledgePoint> listByChapter(Long chapterId) {
        return knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getChapterId, chapterId)
                        .orderByAsc(KnowledgePoint::getSortOrder));
    }

    @Override
    @Transactional
    public void linkQuestionToKnowledgePoints(Long questionId, List<Long> kpIds) {
        questionKnowledgePointMapper.delete(
                new LambdaQueryWrapper<QuestionKnowledgePoint>()
                        .eq(QuestionKnowledgePoint::getQuestionId, questionId));

        if (kpIds != null) {
            for (Long kpId : kpIds) {
                QuestionKnowledgePoint qkp = QuestionKnowledgePoint.builder()
                        .questionId(questionId)
                        .knowledgePointId(kpId)
                        .build();
                questionKnowledgePointMapper.insert(qkp);
            }
        }
    }

    @Override
    public List<KnowledgePointMasteryDTO> getStudentMastery(Long studentId, Long courseId) {
        List<KnowledgePointMastery> masteries = knowledgePointMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgePointMastery>()
                        .eq(KnowledgePointMastery::getStudentId, studentId)
                        .eq(KnowledgePointMastery::getCourseId, courseId));

        if (masteries.isEmpty()) return new ArrayList<>();

        List<Long> kpIds = masteries.stream()
                .map(KnowledgePointMastery::getKnowledgePointId)
                .collect(Collectors.toList());

        List<KnowledgePoint> kps = knowledgePointMapper.selectBatchIds(kpIds);
        Map<Long, String> kpNameMap = kps.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName, (a, b) -> a));

        return masteries.stream().map(m -> KnowledgePointMasteryDTO.builder()
                .id(m.getId())
                .studentId(m.getStudentId())
                .knowledgePointId(m.getKnowledgePointId())
                .knowledgePointName(kpNameMap.getOrDefault(m.getKnowledgePointId(), ""))
                .courseId(m.getCourseId())
                .masteryLevel(m.getMasteryLevel())
                .totalQuestions(m.getTotalQuestions())
                .correctCount(m.getCorrectCount())
                .examAttempts(m.getExamAttempts())
                .status(m.getStatus())
                .lastAssessedAt(m.getLastAssessedAt())
                .build()).collect(Collectors.toList());
    }
}
