package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.LearningPathItem;
import com.training.entity.RemedialTask;
import com.training.mapper.LearningPathItemMapper;
import com.training.mapper.RemedialTaskMapper;
import com.training.service.AuditLogService;
import com.training.service.RemedialTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemedialTaskServiceImpl implements RemedialTaskService {

    private final RemedialTaskMapper remedialTaskMapper;
    private final LearningPathItemMapper learningPathItemMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public RemedialTask create(Long studentId, Long courseId, Long chapterId, Long pathId, String source) {
        // Check for existing pending/in-progress task for same student+chapter
        RemedialTask existing = remedialTaskMapper.selectOne(
                new LambdaQueryWrapper<RemedialTask>()
                        .eq(RemedialTask::getStudentId, studentId)
                        .eq(RemedialTask::getChapterId, chapterId)
                        .in(RemedialTask::getStatus, "PENDING", "IN_PROGRESS"));
        if (existing != null) {
            return existing;
        }

        RemedialTask task = RemedialTask.builder()
                .studentId(studentId)
                .courseId(courseId)
                .chapterId(chapterId)
                .pathId(pathId)
                .source(source)
                .status("PENDING")
                .build();
        remedialTaskMapper.insert(task);

        auditLogService.log(null, null, "TASK_CREATED", "REMEDIAL_TASK", task.getId(),
                "{\"studentId\":" + studentId + ",\"chapterId\":" + chapterId + ",\"source\":\"" + source + "\"}");

        return task;
    }

    @Override
    public RemedialTask getById(Long id) {
        RemedialTask task = remedialTaskMapper.selectById(id);
        if (task == null) throw new BusinessException("补学任务不存在");
        return task;
    }

    @Override
    public List<RemedialTask> listByStudent(Long studentId, String status) {
        LambdaQueryWrapper<RemedialTask> wrapper = new LambdaQueryWrapper<RemedialTask>()
                .eq(RemedialTask::getStudentId, studentId);
        if (status != null) wrapper.eq(RemedialTask::getStatus, status);
        wrapper.orderByDesc(RemedialTask::getCreatedAt);
        return remedialTaskMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void start(Long id) {
        RemedialTask task = getById(id);
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("只能开始待处理的补学任务");
        }
        task.setStatus("IN_PROGRESS");
        task.setStartedAt(LocalDateTime.now());
        remedialTaskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void complete(Long id) {
        RemedialTask task = getById(id);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("补学任务已完成");
        }
        if ("CANCELLED".equals(task.getStatus())) {
            throw new BusinessException("补学任务已取消");
        }
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        remedialTaskMapper.updateById(task);

        // Update linked path item if exists
        if (task.getPathId() != null) {
            LearningPathItem item = learningPathItemMapper.selectOne(
                    new LambdaQueryWrapper<LearningPathItem>()
                            .eq(LearningPathItem::getPathId, task.getPathId())
                            .eq(LearningPathItem::getItemType, "REMEDIAL")
                            .eq(LearningPathItem::getRefId, id));
            if (item != null) {
                item.setStatus("COMPLETED");
                item.setCompletedAt(LocalDateTime.now());
                learningPathItemMapper.updateById(item);
            }
        }

        auditLogService.log(null, null, "TASK_COMPLETED", "REMEDIAL_TASK", id,
                "{\"studentId\":" + task.getStudentId() + ",\"chapterId\":" + task.getChapterId() + "}");
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        RemedialTask task = getById(id);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("已完成的补学任务不能取消");
        }
        task.setStatus("CANCELLED");
        remedialTaskMapper.updateById(task);

        // Cancel linked path item if exists
        if (task.getPathId() != null) {
            LearningPathItem item = learningPathItemMapper.selectOne(
                    new LambdaQueryWrapper<LearningPathItem>()
                            .eq(LearningPathItem::getPathId, task.getPathId())
                            .eq(LearningPathItem::getItemType, "REMEDIAL")
                            .eq(LearningPathItem::getRefId, id));
            if (item != null) {
                item.setStatus("CANCELLED");
                learningPathItemMapper.updateById(item);
            }
        }
    }
}
