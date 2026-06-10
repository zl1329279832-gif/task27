package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.SysUser;
import com.training.entity.dto.UserRequest;

public interface UserService {

    IPage<SysUser> list(int page, int size, String keyword, String role);

    SysUser getById(Long id);

    SysUser create(UserRequest req);

    SysUser update(Long id, UserRequest req);

    void delete(Long id);

    void updateStatus(Long id, String status);

    void resetPassword(Long id, String newPassword);
}
