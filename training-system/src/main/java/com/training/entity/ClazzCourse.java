package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("clazz_course")
public class ClazzCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clazzId;

    private Long courseId;

    private LocalDate startDate;

    private LocalDate endDate;
}
