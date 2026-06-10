package com.training.service;

import com.training.dto.response.PageResult;
import com.training.entity.SysUser;

import java.util.List;

public interface UserService {

    PageResult<SysUser> listUsers(String keyword, Integer status, int page, int size);

    SysUser getUserById(Long id);

    void updateUser(Long id, SysUser user);

    void deleteUser(Long id);

    void assignRole(Long userId, String roleCode);

    void removeRole(Long userId, String roleCode);

    List<String> getUserRoles(Long userId);
}
