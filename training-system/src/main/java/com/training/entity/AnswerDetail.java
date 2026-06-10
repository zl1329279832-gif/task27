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

    /** Correct answer frozen at exam start time (from question snapshot) */
    private String correctAnswer;

    /** Question score frozen at exam start time (from snapshot or scoreOverride) */
    private Integer snapshotScore;

    private Integer isCorrect;

    private Double scoreEarned;

    private String gradingNote;
}
