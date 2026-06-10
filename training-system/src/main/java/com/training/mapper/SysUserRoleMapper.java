package com.training.mapper;

import com.training.entity.SysUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserRoleMapper {

    List<SysUserRole> selectByUserId(Long userId);

    int insert(SysUserRole userRole);

    int deleteByUserId(Long userId);

    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
