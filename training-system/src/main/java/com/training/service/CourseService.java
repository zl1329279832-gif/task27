package com.training.service;

import com.training.dto.request.CourseRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Course;

public interface CourseService {

    PageResult<Course> listCourses(String keyword, String status, Long instructorId, int page, int size);

    Course getCourseById(Long id);

    Course createCourse(CourseRequest request, Long instructorId);

    void updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);

    void publishCourse(Long id);

    void archiveCourse(Long id);
}
