package com.training.mapper;

import com.training.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {

    SysUser selectById(Long id);

    SysUser selectByUsername(String username);

    List<SysUser> selectList(@Param("keyword") String keyword, @Param("status") Integer status);

    int insert(SysUser user);

    int updateById(SysUser user);

    int deleteById(Long id);

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
