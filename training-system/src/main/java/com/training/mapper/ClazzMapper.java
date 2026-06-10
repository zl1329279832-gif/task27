package com.training.mapper;

import com.training.entity.Clazz;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClazzMapper {

    Clazz selectById(Long id);

    List<Clazz> selectList(@Param("keyword") String keyword, @Param("status") String status, @Param("courseId") Long courseId);

    int insert(Clazz clazz);

    int updateById(Clazz clazz);

    int deleteById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
