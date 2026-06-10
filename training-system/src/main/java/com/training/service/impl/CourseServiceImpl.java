package com.training.service.impl;

import com.github.pagehelper.PageHelper;
import com.training.dto.request.CourseRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Course;
import com.training.enums.CourseStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ChapterMapper;
import com.training.mapper.CourseMapper;
import com.training.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;

    @Override
    public PageResult<Course> listCourses(String keyword, String status, Long instructorId, int page, int size) {
        log.info("查询课程列表, keyword: {}, status: {}, instructorId: {}, page: {}, size: {}",
                keyword, status, instructorId, page, size);
        PageHelper.startPage(page, size);
        List<Course> list = courseMapper.selectList(keyword, status, instructorId);
        return PageResult.of(list);
    }

    @Override
    public Course getCourseById(Long id) {
        log.info("查询课程详情, id: {}", id);
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    @Override
    public Course createCourse(CourseRequest request, Long instructorId) {
        log.info("创建课程, title: {}, instructorId: {}", request.getTitle(), instructorId);
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setCoverImage(request.getCoverImage());
        course.setInstructorId(instructorId);
        course.setStatus(CourseStatus.DRAFT.name());
        courseMapper.insert(course);
        return course;
    }

    @Override
    public void updateCourse(Long id, CourseRequest request) {
        log.info("更新课程, id: {}", id);
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setCoverImage(request.getCoverImage());
        courseMapper.updateById(course);
    }

    @Override
    public void deleteCourse(Long id) {
        log.info("删除课程, id: {}", id);
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        int chapterCount = chapterMapper.countByCourseId(id);
        if (chapterCount > 0) {
            throw new BusinessException("请先删除课程下的章节");
        }
        courseMapper.deleteById(id);
    }

    @Override
    public void publishCourse(Long id) {
        log.info("发布课程, id: {}", id);
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!CourseStatus.DRAFT.name().equals(course.getStatus())) {
            throw new BusinessException("只有草稿状态的课程才能发布");
        }
        course.setStatus(CourseStatus.PUBLISHED.name());
        courseMapper.updateById(course);
    }

    @Override
    public void archiveCourse(Long id) {
        log.info("归档课程, id: {}", id);
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!CourseStatus.PUBLISHED.name().equals(course.getStatus())) {
            throw new BusinessException("只有已发布的课程才能归档");
        }
        course.setStatus(CourseStatus.ARCHIVED.name());
        courseMapper.updateById(course);
    }
}
