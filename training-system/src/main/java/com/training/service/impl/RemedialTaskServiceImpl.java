package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.RemedialTask;
import com.training.mapper.RemedialTaskMapper;
import com.training.service.RemedialTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemedialTaskServiceImpl implements RemedialTaskService {

    private final RemedialTaskMapper remedialTaskMapper;

    @Override
    @Transactional
    public RemedialTask create(RemedialTask task) {
        task.setStatus("PENDING");
        task.setAchievedProgress(BigDecimal.ZERO);
        remedialTaskMapper.insert(task);
        return task;
    }

    @Override
    public IPage<RemedialTask> listByStudent(Long studentId, int page, int size) {
        return remedialTaskMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<RemedialTask>()
                        .eq(RemedialTask::getStudentId, studentId)
                        .orderByDesc(RemedialTask::getCreatedAt));
    }

    @Override
    @Transactional
    public void markProgress(Long taskId, double achievedProgress) {
        RemedialTask task = remedialTaskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("补学任务不存在");
        if ("COMPLETED".equals(task.getStatus()) || "EXPIRED".equals(task.getStatus()))
            throw new BusinessException("任务已完成或已过期，无法更新进度");

        task.setAchievedProgress(BigDecimal.valueOf(achievedProgress));
        if ("PENDING".equals(task.getStatus())) {
            task.setStatus("IN_PROGRESS");
        }
        if (achievedProgress >= task.getRequiredProgress().doubleValue()) {
            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
        }
        remedialTaskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void complete(Long taskId) {
        RemedialTask task = remedialTaskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("补学任务不存在");
        task.setStatus("COMPLETED");
        task.setAchievedProgress(task.getRequiredProgress());
        task.setCompletedAt(LocalDateTime.now());
        remedialTaskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void expire(Long taskId) {
        RemedialTask task = remedialTaskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("补学任务不存在");
        task.setStatus("EXPIRED");
        remedialTaskMapper.updateById(task);
    }
}
