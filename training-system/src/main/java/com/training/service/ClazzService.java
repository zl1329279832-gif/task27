package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.Clazz;
import com.training.entity.Course;
import com.training.entity.SysUser;
import com.training.entity.dto.ClazzRequest;

import java.time.LocalDate;
import java.util.List;

public interface ClazzService {

    IPage<Clazz> list(int page, int size, String keyword, String status);

    Clazz getById(Long id);

    Clazz create(ClazzRequest req);

    Clazz update(Long id, ClazzRequest req);

    void delete(Long id);

    void addStudent(Long clazzId, Long studentId);

    void removeStudent(Long clazzId, Long studentId);

    void addCourse(Long clazzId, Long courseId, LocalDate startDate, LocalDate endDate);

    void removeCourse(Long clazzId, Long courseId);

    List<SysUser> listStudents(Long clazzId);

    List<Course> listCourses(Long clazzId);
}
