package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.LearningPathDTO;
import com.training.entity.dto.LearningPathItemDTO;
import com.training.mapper.*;
import com.training.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathMapper learningPathMapper;
    private final LearningPathItemMapper learningPathItemMapper;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final RemedialTaskService remedialTaskService;
    private final MakeupExamTaskService makeupExamTaskService;
    private final AuditLogService auditLogService;
    private final GradeMapper gradeMapper;
    private final ExamMapper examMapper;
    private final ChapterMapper chapterMapper;

    @Override
    @Transactional
    public LearningPath generatePath(Long studentId, Long courseId, String reason) {
        // 1. Refresh mastery data
        List<KnowledgeMastery> masteries = knowledgeMasteryService.evaluateMastery(studentId, courseId);

        // 2. Expire existing active paths
        List<LearningPath> activePaths = learningPathMapper.selectList(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStudentId, studentId)
                        .eq(LearningPath::getCourseId, courseId)
                        .in(LearningPath::getStatus, "GENERATED", "IN_PROGRESS"));
        for (LearningPath p : activePaths) {
            p.setStatus("EXPIRED");
            learningPathMapper.updateById(p);
        }

        // 3. Create new path
        LearningPath path = LearningPath.builder()
                .studentId(studentId)
                .courseId(courseId)
                .title("自适应学习路径")
                .status("GENERATED")
                .totalItems(0)
                .completedItems(0)
                .generatedReason(reason)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();
        learningPathMapper.insert(path);

        int sortOrder = 0;

        // 4. Create remedial tasks for unmastered chapters
        for (KnowledgeMastery m : masteries) {
            if ("NOT_MASTERED".equals(m.getMasteryLevel()) || "PARTIALLY_MASTERED".equals(m.getMasteryLevel())) {
                RemedialTask task = remedialTaskService.create(
                        studentId, courseId, m.getChapterId(), path.getId(),
                        "RENEWAL".equals(reason) ? "RENEWAL" : "MASTERY_CHECK");

                LearningPathItem item = LearningPathItem.builder()
                        .pathId(path.getId())
                        .itemType("REMEDIAL")
                        .refId(task.getId())
                        .sortOrder(++sortOrder)
                        .status("PENDING")
                        .build();
                learningPathItemMapper.insert(item);
            }
        }

        // 5. Create makeup exam tasks for failed exams
        List<Grade> failedGrades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, studentId)
                        .eq(Grade::getCourseId, courseId)
                        .eq(Grade::getPass, 0));

        // Get distinct exam IDs with failed grades (exclude exams that were later passed)
        List<Grade> passedGrades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, studentId)
                        .eq(Grade::getCourseId, courseId)
                        .eq(Grade::getPass, 1));
        Set<Long> passedExamIds = passedGrades.stream()
                .map(Grade::getExamId).collect(Collectors.toSet());

        Set<Long> failedExamIds = failedGrades.stream()
                .map(Grade::getExamId)
                .filter(eid -> !passedExamIds.contains(eid))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Long examId : failedExamIds) {
            MakeupExamTask task = makeupExamTaskService.create(
                    studentId, courseId, examId, path.getId(),
                    "RENEWAL".equals(reason) ? "RENEWAL" : "EXAM_FAILED");

            LearningPathItem item = LearningPathItem.builder()
                    .pathId(path.getId())
                    .itemType("MAKEUP_EXAM")
                    .refId(task.getId())
                    .sortOrder(++sortOrder)
                    .status("PENDING")
                    .build();
            learningPathItemMapper.insert(item);
        }

        // 6. Update total items
        path.setTotalItems(sortOrder);
        if (sortOrder > 0) {
            path.setStatus("IN_PROGRESS");
        } else {
            path.setStatus("COMPLETED");
            path.setCompletedAt(LocalDateTime.now());
        }
        learningPathMapper.updateById(path);

        auditLogService.log(null, null, "PATH_GENERATED", "LEARNING_PATH", path.getId(),
                "{\"studentId\":" + studentId + ",\"courseId\":" + courseId
                        + ",\"reason\":\"" + reason + "\",\"totalItems\":" + sortOrder + "}");

        return path;
    }

    @Override
    public LearningPathDTO getActivePath(Long studentId, Long courseId) {
        LearningPath path = learningPathMapper.selectOne(
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStudentId, studentId)
                        .eq(LearningPath::getCourseId, courseId)
                        .in(LearningPath::getStatus, "GENERATED", "IN_PROGRESS")
                        .orderByDesc(LearningPath::getCreatedAt)
                        .last("LIMIT 1"));
        if (path == null) return null;

        List<LearningPathItem> items = learningPathItemMapper.selectList(
                new LambdaQueryWrapper<LearningPathItem>()
                        .eq(LearningPathItem::getPathId, path.getId())
                        .orderByAsc(LearningPathItem::getSortOrder));

        // Build chapter/exam title lookups
        Map<Long, String> chapterTitles = new HashMap<>();
        Map<Long, String> examTitles = new HashMap<>();

        List<Long> remedialRefIds = items.stream()
                .filter(i -> "REMEDIAL".equals(i.getItemType()))
                .map(LearningPathItem::getRefId).collect(Collectors.toList());
        List<Long> makeupRefIds = items.stream()
                .filter(i -> "MAKEUP_EXAM".equals(i.getItemType()))
                .map(LearningPathItem::getRefId).collect(Collectors.toList());

        // Look up chapter titles from remedial tasks
        if (!remedialRefIds.isEmpty()) {
            // refId references remedial_task.id; we need the chapter title
            List<Chapter> chapters = chapterMapper.selectList(
                    new LambdaQueryWrapper<Chapter>()
                            .eq(Chapter::getCourseId, courseId));
            for (Chapter ch : chapters) {
                chapterTitles.put(ch.getId(), ch.getTitle());
            }
        }

        List<LearningPathItemDTO> itemDTOs = items.stream().map(item ->
                LearningPathItemDTO.builder()
                        .id(item.getId())
                        .pathId(item.getPathId())
                        .itemType(item.getItemType())
                        .refId(item.getRefId())
                        .sortOrder(item.getSortOrder())
                        .status(item.getStatus())
                        .completedAt(item.getCompletedAt())
                        .build()
        ).collect(Collectors.toList());

        return LearningPathDTO.builder()
                .id(path.getId())
                .studentId(path.getStudentId())
                .courseId(path.getCourseId())
                .title(path.getTitle())
                .status(path.getStatus())
                .totalItems(path.getTotalItems())
                .completedItems(path.getCompletedItems())
                .generatedReason(path.getGeneratedReason())
                .expiresAt(path.getExpiresAt())
                .completedAt(path.getCompletedAt())
                .items(itemDTOs)
                .build();
    }

    @Override
    public IPage<LearningPath> listByStudent(Long studentId, int page, int size) {
        return learningPathMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LearningPath>()
                        .eq(LearningPath::getStudentId, studentId)
                        .orderByDesc(LearningPath::getCreatedAt));
    }

    @Override
    @Transactional
    public void updateItemStatus(Long pathItemId, String status) {
        LearningPathItem item = learningPathItemMapper.selectById(pathItemId);
        if (item == null) return;

        item.setStatus(status);
        if ("COMPLETED".equals(status)) {
            item.setCompletedAt(LocalDateTime.now());
        }
        learningPathItemMapper.updateById(item);

        checkPathCompletion(item.getPathId());
    }

    @Override
    @Transactional
    public void checkPathCompletion(Long pathId) {
        LearningPath path = learningPathMapper.selectById(pathId);
        if (path == null || "COMPLETED".equals(path.getStatus()) || "EXPIRED".equals(path.getStatus())) {
            return;
        }

        List<LearningPathItem> items = learningPathItemMapper.selectList(
                new LambdaQueryWrapper<LearningPathItem>()
                        .eq(LearningPathItem::getPathId, pathId));

        long completedCount = items.stream()
                .filter(i -> "COMPLETED".equals(i.getStatus()))
                .count();
        long cancelledCount = items.stream()
                .filter(i -> "CANCELLED".equals(i.getStatus()))
                .count();

        path.setCompletedItems((int) completedCount);

        if (completedCount + cancelledCount >= items.size()) {
            path.setStatus("COMPLETED");
            path.setCompletedAt(LocalDateTime.now());
        }

        learningPathMapper.updateById(path);
    }
}
