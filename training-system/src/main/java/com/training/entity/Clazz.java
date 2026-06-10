package com.training.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clazz {

    private Long id;

    private String name;

    private Long courseId;

    private Long instructorId;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer maxStudents;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
