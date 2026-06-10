package com.training.mapper;

import com.training.entity.SysRole;

import java.util.List;

public interface SysRoleMapper {

    SysRole selectById(Long id);

    SysRole selectByRoleCode(String roleCode);

    List<SysRole> selectAll();

    int insert(SysRole role);
}
