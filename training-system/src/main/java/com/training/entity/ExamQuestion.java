package com.training.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion {

    private Long id;

    private Long examId;

    private Long questionId;

    private String questionSnapshot;

    private Integer sortOrder;

    private BigDecimal score;
}
