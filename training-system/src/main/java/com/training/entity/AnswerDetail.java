package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "answer_detail", autoResultMap = true)
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

    // --- Snapshot fields: frozen at exam start to guarantee fairness ---

    private String snapshotContent;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> snapshotOptions;

    private String snapshotCorrectAnswer;

    private String snapshotQuestionType;

    private Integer snapshotScore;
}
