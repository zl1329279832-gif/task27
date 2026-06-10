package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.Course;
import com.training.entity.dto.CourseRequest;
import com.training.mapper.CourseMapper;
import com.training.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;

    @Override
    public IPage<Course> list(int page, int size, String keyword, String status) {
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("title", keyword);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("created_at");
        return courseMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    @Override
    public Course getById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
    }

    @Override
    @Transactional
    public Course create(CourseRequest req, Long instructorId) {
        Course course = new Course();
        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setCoverImage(req.getCoverImage());
        course.setCategory(req.getCategory());
        course.setDifficulty(req.getDifficulty());
        course.setTotalHours(req.getTotalHours());
        course.setInstructorId(instructorId);
        course.setStatus("DRAFT");
        courseMapper.insert(course);
        return course;
    }

    @Override
    @Transactional
    public Course update(Long id, CourseRequest req) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setCoverImage(req.getCoverImage());
        course.setCategory(req.getCategory());
        course.setDifficulty(req.getDifficulty());
        course.setTotalHours(req.getTotalHours());
        courseMapper.updateById(course);
        return course;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!"DRAFT".equals(course.getStatus())) {
            throw new BusinessException("只有草稿状态的课程可以删除");
        }
        courseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!"DRAFT".equals(course.getStatus())) {
            throw new BusinessException("只有草稿状态的课程可以发布");
        }
        course.setStatus("PUBLISHED");
        courseMapper.updateById(course);
    }

    @Override
    @Transactional
    public void archive(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        course.setStatus("ARCHIVED");
        courseMapper.updateById(course);
    }
}
