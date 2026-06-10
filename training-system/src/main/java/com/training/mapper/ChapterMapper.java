package com.training.mapper;

import com.training.entity.Chapter;

import java.util.List;

public interface ChapterMapper {

    Chapter selectById(Long id);

    List<Chapter> selectByCourseId(Long courseId);

    int insert(Chapter chapter);

    int updateById(Chapter chapter);

    int deleteById(Long id);

    int deleteByCourseId(Long courseId);

    int countByCourseId(Long courseId);
}
