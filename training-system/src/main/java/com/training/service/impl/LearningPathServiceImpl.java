package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.LearningPathDTO;
import com.training.entity.dto.ChapterProgressDTO;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.LearningPathService;
import com.training.service.LearningRecordService;
import com.training.service.KnowledgePointMasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathMapper learningPathMapper;
    private final RemedialTaskMapper remedialTaskMapper;
    private final MakeupExamMapper makeupExamMapper;
    private final AnswerSheetMapper answerSheetMapper;
    private final CertificateMapper certificateMapper;
    private final ChapterMapper chapterMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final LearningRecordService learningRecordService;
    private final KnowledgePointMasteryService knowledgePointMasteryService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public LearningPath generatePath(Long studentId, Long courseId, String triggerReason, Long operatorId) {
        List<LearningPath.PathStep> steps = new ArrayList<>();
        int stepOrder = 1;

        // 1. Incomplete chapters -> CHAPTER_STUDY steps
        try {
            List<ChapterProgressDTO> chapterProgressList = learningRecordService.getChapterProgress(studentId, courseId);
            if (chapterProgressList != null) {
                for (var cp : chapterProgressList) {
                    if (cp.getProgress() < 100) {
                        steps.add(LearningPath.PathStep.builder()
                                .stepOrder(stepOrder++)
                                .stepType("CHAPTER_STUDY")
                                .targetId(cp.getChapterId())
                                .targetTitle(cp.getChapterTitle())
                                .status("PENDING")
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取章节进度失败: studentId={}, courseId={}", studentId, courseId, e);
        }

        // 2. Unmastered knowledge points -> MAKEUP_CHAPTER steps
        try {
            List<KnowledgePointMastery> unmastered =
                    knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0);
            for (KnowledgePointMastery mastery : unmastered) {
                KnowledgePoint kp = knowledgePointMapper.selectById(mastery.getKnowledgePointId());
                if (kp != null && kp.getChapterId() != null) {
                    // Check if this chapter is already in steps
                    boolean alreadyInSteps = steps.stream()
                            .anyMatch(s -> "CHAPTER_STUDY".equals(s.getStepType())
                                    && kp.getChapterId().equals(s.getTargetId()));
                    if (!alreadyInSteps) {
                        Chapter chapter = chapterMapper.selectById(kp.getChapterId());
                        steps.add(LearningPath.PathStep.builder()
                                .stepOrder(stepOrder++)
                                .stepType("MAKEUP_CHAPTER")
                                .targetId(kp.getChapterId())
                                .targetTitle(chapter != null ? chapter.getTitle() : kp.getName())
                                .status("PENDING")
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取未掌握知识点失败: studentId={}, courseId={}", studentId, courseId, e);
        }

        // 3. Failed exams -> MAKEUP_EXAM steps
        List<AnswerSheet> failedSheets = answerSheetMapper.selectList(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getStudentId, studentId)
                        .eq(AnswerSheet::getPass, 0)
                        .ne(AnswerSheet::getStatus, "IN_PROGRESS"));

        Set<Long> processedExamIds = new HashSet<>();
        for (AnswerSheet sheet : failedSheets) {
            if (processedExamIds.contains(sheet.getExamId())) continue;
            processedExamIds.add(sheet.getExamId());
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(stepOrder++)
                    .stepType("MAKEUP_EXAM")
                    .targetId(sheet.getExamId())
                    .targetTitle("补考: 考试#" + sheet.getExamId())
                    .status("PENDING")
                    .build());
        }

        // 4. High tab-switch count -> REVIEW steps
        List<AnswerSheet> highTabSwitch = answerSheetMapper.selectList(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getStudentId, studentId)
                        .gt(AnswerSheet::getTabSwitchCount, 3));

        if (!highTabSwitch.isEmpty()) {
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(stepOrder++)
                    .stepType("REVIEW")
                    .targetId(courseId)
                    .targetTitle("诚信复习: 检测到多次切屏行为")
                    .status("PENDING")
                    .build());
        }

        // Expire previous active paths
        List<LearningPath> activePaths = learningPathMapper.selectList(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStudentId, studentId)
                        .eq(LearningPath::getCourseId, courseId)
                        .in(LearningPath::getStatus, "GENERATED", "IN_PROGRESS"));
        for (LearningPath oldPath : activePaths) {
            oldPath.setStatus("EXPIRED");
            learningPathMapper.updateById(oldPath);
        }

        // Create new path
        String reason = triggerReason != null ? triggerReason : "INITIAL";
        LearningPath path = LearningPath.builder()
                .studentId(studentId)
                .courseId(courseId)
                .status("GENERATED")
                .triggerReason(reason)
                .pathData(steps)
                .totalSteps(steps.size())
                .completedSteps(0)
                .generatedBy(operatorId)
                .generatedAt(LocalDateTime.now())
                .build();
        learningPathMapper.insert(path);

        // Create RemedialTask records for MAKEUP_CHAPTER steps
        for (LearningPath.PathStep step : steps) {
            if ("MAKEUP_CHAPTER".equals(step.getStepType())) {
                RemedialTask task = RemedialTask.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .learningPathId(path.getId())
                        .chapterId(step.getTargetId())
                        .taskType("REMEDIAL_CHAPTER")
                        .status("PENDING")
                        .requiredProgress(BigDecimal.valueOf(100))
                        .achievedProgress(BigDecimal.ZERO)
                        .build();
                remedialTaskMapper.insert(task);
            }
        }

        // Create MakeupExam records for MAKEUP_EXAM steps
        for (LearningPath.PathStep step : steps) {
            if ("MAKEUP_EXAM".equals(step.getStepType())) {
                MakeupExam makeup = MakeupExam.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .examId(step.getTargetId())
                        .learningPathId(path.getId())
                        .status("PENDING")
                        .maxAttempts(2)
                        .attemptsUsed(0)
                        .requiredScore(BigDecimal.valueOf(60))
                        .build();
                makeupExamMapper.insert(makeup);
            }
        }

        auditLogService.log("PATH_GENERATED", "LEARNING_PATH", path.getId(),
                operatorId != null ? operatorId : studentId, null,
                Map.of("studentId", studentId, "courseId", courseId,
                        "triggerReason", reason, "totalSteps", steps.size()));

        return path;
    }

    @Override
    public LearningPathDTO getActivePath(Long studentId, Long courseId) {
        LearningPath path = learningPathMapper.selectOne(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStudentId, studentId)
                        .eq(LearningPath::getCourseId, courseId)
                        .in(LearningPath::getStatus, "GENERATED", "IN_PROGRESS")
                        .orderByDesc(LearningPath::getGeneratedAt)
                        .last("LIMIT 1"));

        if (path == null) return null;

        return LearningPathDTO.builder()
                .id(path.getId())
                .studentId(path.getStudentId())
                .courseId(path.getCourseId())
                .status(path.getStatus())
                .triggerReason(path.getTriggerReason())
                .totalSteps(path.getTotalSteps())
                .completedSteps(path.getCompletedSteps())
                .steps(path.getPathData())
                .generatedAt(path.getGeneratedAt())
                .completedAt(path.getCompletedAt())
                .expiresAt(path.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public void completePathStep(Long pathId, Integer stepOrder) {
        LearningPath path = learningPathMapper.selectById(pathId);
        if (path == null) throw new BusinessException("学习路径不存在");
        if ("EXPIRED".equals(path.getStatus())) throw new BusinessException("学习路径已过期");

        List<LearningPath.PathStep> steps = path.getPathData();
        if (steps == null) throw new BusinessException("路径步骤为空");

        LearningPath.PathStep targetStep = null;
        for (LearningPath.PathStep step : steps) {
            if (step.getStepOrder().equals(stepOrder)) {
                targetStep = step;
                break;
            }
        }
        if (targetStep == null) throw new BusinessException("路径步骤不存在");
        if ("COMPLETED".equals(targetStep.getStatus())) throw new BusinessException("步骤已完成");

        targetStep.setStatus("COMPLETED");
        targetStep.setCompletedAt(LocalDateTime.now());

        int completedCount = 0;
        for (LearningPath.PathStep step : steps) {
            if ("COMPLETED".equals(step.getStatus())) completedCount++;
        }
        path.setCompletedSteps(completedCount);
        path.setStatus("IN_PROGRESS");

        if (completedCount >= path.getTotalSteps()) {
            path.setStatus("COMPLETED");
            path.setCompletedAt(LocalDateTime.now());
        }

        path.setPathData(steps);
        learningPathMapper.updateById(path);

        // Sync RemedialTask status for MAKEUP_CHAPTER steps
        if ("MAKEUP_CHAPTER".equals(targetStep.getStepType())) {
            RemedialTask task = remedialTaskMapper.selectOne(
                    new LambdaQueryWrapper<RemedialTask>()
                            .eq(RemedialTask::getLearningPathId, pathId)
                            .eq(RemedialTask::getChapterId, targetStep.getTargetId())
                            .eq(RemedialTask::getStatus, "PENDING"));
            if (task != null) {
                task.setStatus("COMPLETED");
                task.setCompletedAt(LocalDateTime.now());
                task.setAchievedProgress(task.getRequiredProgress());
                remedialTaskMapper.updateById(task);
            }
        }
    }

    @Override
    public IPage<LearningPath> listPaths(Long studentId, int page, int size) {
        return learningPathMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStudentId, studentId)
                        .orderByDesc(LearningPath::getGeneratedAt));
    }

    @Override
    @Transactional
    public void expirePath(Long pathId) {
        LearningPath path = learningPathMapper.selectById(pathId);
        if (path == null) throw new BusinessException("学习路径不存在");
        path.setStatus("EXPIRED");
        learningPathMapper.updateById(path);

        // Expire associated remedial tasks and makeup exams
        List<RemedialTask> tasks = remedialTaskMapper.selectList(
                new LambdaQueryWrapper<RemedialTask>()
                        .eq(RemedialTask::getLearningPathId, pathId)
                        .in(RemedialTask::getStatus, "PENDING", "IN_PROGRESS"));
        for (RemedialTask task : tasks) {
            task.setStatus("EXPIRED");
            remedialTaskMapper.updateById(task);
        }

        List<MakeupExam> exams = makeupExamMapper.selectList(
                new LambdaQueryWrapper<MakeupExam>()
                        .eq(MakeupExam::getLearningPathId, pathId)
                        .in(MakeupExam::getStatus, "PENDING", "IN_PROGRESS"));
        for (MakeupExam exam : exams) {
            exam.setStatus("EXPIRED");
            makeupExamMapper.updateById(exam);
        }
    }
}
