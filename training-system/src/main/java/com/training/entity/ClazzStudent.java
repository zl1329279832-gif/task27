package com.training.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClazzStudent {

    private Long id;

    private Long clazzId;

    private Long studentId;

    private LocalDateTime joinedAt;
}
