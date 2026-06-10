package com.training.mapper;

import com.training.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper {

    Course selectById(Long id);

    List<Course> selectList(@Param("keyword") String keyword, @Param("status") String status, @Param("instructorId") Long instructorId);

    int insert(Course course);

    int updateById(Course course);

    int deleteById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
