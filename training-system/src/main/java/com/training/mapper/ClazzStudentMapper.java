package com.training.mapper;

import com.training.entity.ClazzStudent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClazzStudentMapper {

    List<ClazzStudent> selectByClazzId(Long clazzId);

    List<ClazzStudent> selectByStudentId(Long studentId);

    ClazzStudent selectByClazzAndStudent(@Param("clazzId") Long clazzId, @Param("studentId") Long studentId);

    int insert(ClazzStudent clazzStudent);

    int deleteById(Long id);

    int deleteByClazzAndStudent(@Param("clazzId") Long clazzId, @Param("studentId") Long studentId);

    int countByClazzId(Long clazzId);
}
