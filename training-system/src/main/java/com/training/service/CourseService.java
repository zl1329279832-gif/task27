package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.Course;
import com.training.entity.dto.CourseRequest;

public interface CourseService {

    IPage<Course> list(int page, int size, String keyword, String status);

    Course getById(Long id);

    Course create(CourseRequest req, Long instructorId);

    Course update(Long id, CourseRequest req);

    void delete(Long id);

    void publish(Long id);

    void archive(Long id);
}
