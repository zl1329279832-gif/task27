package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class ClazzRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long courseId;

    private Long instructorId;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer maxStudents;
}
