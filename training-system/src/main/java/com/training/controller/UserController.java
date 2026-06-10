package com.training.controller;

import com.training.dto.response.PageResult;
import com.training.dto.response.Result;
import com.training.entity.SysUser;
import com.training.security.CustomUserDetails;
import com.training.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    private Long getCurrentUserId() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询用户列表: keyword={}, page={}, size={}", keyword, page, size);
        PageResult<SysUser> result = userService.listUsers(keyword, null, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> getById(@PathVariable Long id) {
        log.info("查询用户详情: id={}", id);
        SysUser user = userService.getUserById(id);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        log.info("更新用户: id={}", id);
        user.setId(id);
        userService.updateUser(id, user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除用户: id={}", id);
        userService.deleteUser(id);
        return Result.success();
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignRole(@PathVariable Long id, @RequestParam String roleCode) {
        log.info("分配角色: userId={}, roleCode={}", id, roleCode);
        userService.assignRole(id, roleCode);
        return Result.success();
    }

    @DeleteMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> removeRole(@PathVariable Long id, @RequestParam String roleCode) {
        log.info("移除角色: userId={}, roleCode={}", id, roleCode);
        userService.removeRole(id, roleCode);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<String>> getRoles(@PathVariable Long id) {
        log.info("查询用户角色: userId={}", id);
        List<String> roles = userService.getUserRoles(id);
        return Result.success(roles);
    }

    @GetMapping("/me")
    public Result<SysUser> getCurrentUser() {
        Long userId = getCurrentUserId();
        log.info("查询当前用户信息: userId={}", userId);
        SysUser user = userService.getUserById(userId);
        return Result.success(user);
    }
}
