package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.ClazzRequest;
import com.training.mapper.*;
import com.training.service.ClazzService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClazzServiceImpl implements ClazzService {

    private final ClazzMapper clazzMapper;
    private final ClazzStudentMapper clazzStudentMapper;
    private final ClazzCourseMapper clazzCourseMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;

    @Override
    public IPage<Clazz> list(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Clazz> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Clazz::getName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Clazz::getStatus, status);
        }
        wrapper.orderByDesc(Clazz::getCreatedAt);
        return clazzMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Clazz getById(Long id) {
        Clazz clazz = clazzMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }
        return clazz;
    }

    @Override
    public Clazz create(ClazzRequest req) {
        Clazz clazz = new Clazz();
        clazz.setName(req.getName());
        clazz.setDescription(req.getDescription());
        clazz.setInstructorId(req.getInstructorId());
        clazz.setStatus("ACTIVE");

        clazzMapper.insert(clazz);
        return clazz;
    }

    @Override
    public Clazz update(Long id, ClazzRequest req) {
        Clazz clazz = clazzMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }

        clazz.setName(req.getName());
        clazz.setDescription(req.getDescription());
        clazz.setInstructorId(req.getInstructorId());

        clazzMapper.updateById(clazz);
        return clazz;
    }

    @Override
    public void delete(Long id) {
        Clazz clazz = clazzMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }
        clazzMapper.deleteById(id);
    }

    @Override
    public void addStudent(Long clazzId, Long studentId) {
        // Check clazz exists
        Clazz clazz = clazzMapper.selectById(clazzId);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }

        // Check student exists
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        // Check duplicate
        LambdaQueryWrapper<ClazzStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClazzStudent::getClazzId, clazzId)
                .eq(ClazzStudent::getStudentId, studentId);
        Long count = clazzStudentMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("该学生已在班级中");
        }

        ClazzStudent clazzStudent = new ClazzStudent();
        clazzStudent.setClazzId(clazzId);
        clazzStudent.setStudentId(studentId);
        clazzStudentMapper.insert(clazzStudent);
    }

    @Override
    public void removeStudent(Long clazzId, Long studentId) {
        LambdaQueryWrapper<ClazzStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClazzStudent::getClazzId, clazzId)
                .eq(ClazzStudent::getStudentId, studentId);
        int deleted = clazzStudentMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException("该学生不在此班级中");
        }
    }

    @Override
    public void addCourse(Long clazzId, Long courseId, LocalDate startDate, LocalDate endDate) {
        // Check clazz exists
        Clazz clazz = clazzMapper.selectById(clazzId);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }

        // Check course exists
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // Check duplicate
        LambdaQueryWrapper<ClazzCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClazzCourse::getClazzId, clazzId)
                .eq(ClazzCourse::getCourseId, courseId);
        Long count = clazzCourseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("该课程已添加到班级中");
        }

        ClazzCourse clazzCourse = new ClazzCourse();
        clazzCourse.setClazzId(clazzId);
        clazzCourse.setCourseId(courseId);
        clazzCourse.setStartDate(startDate);
        clazzCourse.setEndDate(endDate);
        clazzCourseMapper.insert(clazzCourse);
    }

    @Override
    public void removeCourse(Long clazzId, Long courseId) {
        LambdaQueryWrapper<ClazzCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClazzCourse::getClazzId, clazzId)
                .eq(ClazzCourse::getCourseId, courseId);
        int deleted = clazzCourseMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException("该课程不在此班级中");
        }
    }

    @Override
    public List<SysUser> listStudents(Long clazzId) {
        // Get all student IDs for this clazz
        LambdaQueryWrapper<ClazzStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClazzStudent::getClazzId, clazzId);
        List<ClazzStudent> clazzStudents = clazzStudentMapper.selectList(wrapper);

        if (clazzStudents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = clazzStudents.stream()
                .map(ClazzStudent::getStudentId)
                .collect(Collectors.toList());

        return sysUserMapper.selectBatchIds(studentIds);
    }

    @Override
    public List<Course> listCourses(Long clazzId) {
        // Get all course IDs for this clazz
        LambdaQueryWrapper<ClazzCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClazzCourse::getClazzId, clazzId);
        List<ClazzCourse> clazzCourses = clazzCourseMapper.selectList(wrapper);

        if (clazzCourses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = clazzCourses.stream()
                .map(ClazzCourse::getCourseId)
                .collect(Collectors.toList());

        return courseMapper.selectBatchIds(courseIds);
    }
}
