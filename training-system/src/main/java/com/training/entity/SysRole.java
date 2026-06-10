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
public class SysRole {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private LocalDateTime createdAt;
}
