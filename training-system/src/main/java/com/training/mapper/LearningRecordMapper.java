package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.training.entity.LearningRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
}
