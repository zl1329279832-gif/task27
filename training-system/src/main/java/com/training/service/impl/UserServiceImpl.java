package com.training.service.impl;

import com.github.pagehelper.PageHelper;
import com.training.dto.response.PageResult;
import com.training.entity.SysRole;
import com.training.entity.SysUser;
import com.training.entity.SysUserRole;
import com.training.exception.BusinessException;
import com.training.mapper.SysRoleMapper;
import com.training.mapper.SysUserMapper;
import com.training.mapper.SysUserRoleMapper;
import com.training.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public PageResult<SysUser> listUsers(String keyword, Integer status, int page, int size) {
        log.info("查询用户列表, keyword: {}, status: {}, page: {}, size: {}", keyword, status, page, size);
        PageHelper.startPage(page, size);
        List<SysUser> list = sysUserMapper.selectList(keyword, status);
        list.forEach(user -> user.setPassword(null));
        return PageResult.of(list);
    }

    @Override
    public SysUser getUserById(Long id) {
        log.info("查询用户详情, id: {}", id);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateUser(Long id, SysUser user) {
        log.info("更新用户, id: {}", id);
        SysUser existingUser = sysUserMapper.selectById(id);
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }
        user.setId(id);
        user.setPassword(null);
        sysUserMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        log.info("删除用户, id: {}", id);
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        sysUserMapper.deleteById(id);
    }

    @Override
    public void assignRole(Long userId, String roleCode) {
        log.info("分配角色, userId: {}, roleCode: {}", userId, roleCode);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        SysRole role = sysRoleMapper.selectByRoleCode(roleCode);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        sysUserRoleMapper.insert(userRole);
    }

    @Override
    public void removeRole(Long userId, String roleCode) {
        log.info("移除角色, userId: {}, roleCode: {}", userId, roleCode);
        SysRole role = sysRoleMapper.selectByRoleCode(roleCode);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        sysUserRoleMapper.deleteByUserIdAndRoleId(userId, role.getId());
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        log.info("查询用户角色, userId: {}", userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectByUserId(userId);
        return userRoles.stream()
                .map(ur -> sysRoleMapper.selectById(ur.getRoleId()))
                .filter(role -> role != null)
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }
}
