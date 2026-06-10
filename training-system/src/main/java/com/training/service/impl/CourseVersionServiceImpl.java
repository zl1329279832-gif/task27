package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.Course;
import com.training.entity.CourseVersionHistory;
import com.training.mapper.CourseMapper;
import com.training.mapper.CourseVersionHistoryMapper;
import com.training.service.CourseVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseVersionServiceImpl implements CourseVersionService {

    private final CourseMapper courseMapper;
    private final CourseVersionHistoryMapper courseVersionHistoryMapper;

    @Override
    @Transactional
    public Course upgradeVersion(Long courseId, String changeSummary, Long operatorId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw new BusinessException("课程不存在");

        int fromVersion = course.getVersion() != null ? course.getVersion() : 1;
        int toVersion = fromVersion + 1;

        course.setVersion(toVersion);
        courseMapper.updateById(course);

        CourseVersionHistory history = CourseVersionHistory.builder()
                .courseId(courseId)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .changeSummary(changeSummary)
                .changedBy(operatorId)
                .changedAt(LocalDateTime.now())
                .build();
        courseVersionHistoryMapper.insert(history);

        return course;
    }

    @Override
    public List<CourseVersionHistory> getVersionHistory(Long courseId) {
        return courseVersionHistoryMapper.selectList(
                new LambdaQueryWrapper<CourseVersionHistory>()
                        .eq(CourseVersionHistory::getCourseId, courseId)
                        .orderByDesc(CourseVersionHistory::getChangedAt));
    }

    @Override
    public int getCurrentVersion(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw new BusinessException("课程不存在");
        return course.getVersion() != null ? course.getVersion() : 1;
    }
}
