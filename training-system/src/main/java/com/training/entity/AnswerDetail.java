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
@TableName("answer_detail")
public class AnswerDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long answerSheetId;

    private Long questionId;

    private Long examQuestionId;

    private String studentAnswer;

    private Integer isCorrect;

    private Double scoreEarned;

    private String gradingNote;
}
