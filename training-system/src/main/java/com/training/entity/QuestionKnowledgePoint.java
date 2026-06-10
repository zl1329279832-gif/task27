package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("question_knowledge_point")
public class QuestionKnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Long knowledgePointId;
}
