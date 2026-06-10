package com.training.mapper;

import com.training.entity.Courseware;

import java.util.List;

public interface CoursewareMapper {

    Courseware selectById(Long id);

    List<Courseware> selectByChapterId(Long chapterId);

    List<Courseware> selectByCourseId(Long courseId);

    int insert(Courseware courseware);

    int updateById(Courseware courseware);

    int deleteById(Long id);

    int deleteByChapterId(Long chapterId);

    int countByCourseId(Long courseId);
}
