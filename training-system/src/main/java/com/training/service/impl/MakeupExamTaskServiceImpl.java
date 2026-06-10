package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.LearningPathItem;
import com.training.entity.MakeupExamTask;
import com.training.mapper.LearningPathItemMapper;
import com.training.mapper.MakeupExamTaskMapper;
import com.training.service.AuditLogService;
import com.training.service.MakeupExamTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MakeupExamTaskServiceImpl implements MakeupExamTaskService {

    private final MakeupExamTaskMapper makeupExamTaskMapper;
    private final LearningPathItemMapper learningPathItemMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public MakeupExamTask create(Long studentId, Long courseId, Long examId, Long pathId, String source) {
        MakeupExamTask task = MakeupExamTask.builder()
                .studentId(studentId)
                .courseId(courseId)
                .examId(examId)
                .pathId(pathId)
                .source(source)
                .status("PENDING")
                .build();
        makeupExamTaskMapper.insert(task);

        auditLogService.log(null, null, "TASK_CREATED", "MAKEUP_EXAM_TASK", task.getId(),
                "{\"studentId\":" + studentId + ",\"examId\":" + examId + ",\"source\":\"" + source + "\"}");

        return task;
    }

    @Override
    public MakeupExamTask getById(Long id) {
        MakeupExamTask task = makeupExamTaskMapper.selectById(id);
        if (task == null) throw new BusinessException("补考任务不存在");
        return task;
    }

    @Override
    public List<MakeupExamTask> listByStudent(Long studentId, String status) {
        LambdaQueryWrapper<MakeupExamTask> wrapper = new LambdaQueryWrapper<MakeupExamTask>()
                .eq(MakeupExamTask::getStudentId, studentId);
        if (status != null) wrapper.eq(MakeupExamTask::getStatus, status);
        wrapper.orderByDesc(MakeupExamTask::getCreatedAt);
        return makeupExamTaskMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void schedule(Long id, LocalDateTime scheduledAt) {
        MakeupExamTask task = getById(id);
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("只能安排待处理的补考任务");
        }
        task.setStatus("SCHEDULED");
        task.setScheduledAt(scheduledAt);
        makeupExamTaskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void complete(Long id, Long answerSheetId) {
        MakeupExamTask task = getById(id);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("补考任务已完成");
        }
        if ("CANCELLED".equals(task.getStatus())) {
            throw new BusinessException("补考任务已取消");
        }
        task.setStatus("COMPLETED");
        task.setAnswerSheetId(answerSheetId);
        task.setCompletedAt(LocalDateTime.now());
        makeupExamTaskMapper.updateById(task);

        // Update linked path item if exists
        if (task.getPathId() != null) {
            LearningPathItem item = learningPathItemMapper.selectOne(
                    new LambdaQueryWrapper<LearningPathItem>()
                            .eq(LearningPathItem::getPathId, task.getPathId())
                            .eq(LearningPathItem::getItemType, "MAKEUP_EXAM")
                            .eq(LearningPathItem::getRefId, id));
            if (item != null) {
                item.setStatus("COMPLETED");
                item.setCompletedAt(LocalDateTime.now());
                learningPathItemMapper.updateById(item);
            }
        }

        auditLogService.log(null, null, "TASK_COMPLETED", "MAKEUP_EXAM_TASK", id,
                "{\"studentId\":" + task.getStudentId() + ",\"examId\":" + task.getExamId() + "}");
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        MakeupExamTask task = getById(id);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("已完成的补考任务不能取消");
        }
        task.setStatus("CANCELLED");
        makeupExamTaskMapper.updateById(task);

        if (task.getPathId() != null) {
            LearningPathItem item = learningPathItemMapper.selectOne(
                    new LambdaQueryWrapper<LearningPathItem>()
                            .eq(LearningPathItem::getPathId, task.getPathId())
                            .eq(LearningPathItem::getItemType, "MAKEUP_EXAM")
                            .eq(LearningPathItem::getRefId, id));
            if (item != null) {
                item.setStatus("CANCELLED");
                learningPathItemMapper.updateById(item);
            }
        }
    }
}
