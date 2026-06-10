package com.training.service.impl;
import com.training.security.JwtTokenProvider;

import com.training.dto.request.LoginRequest;
import com.training.dto.request.RegisterRequest;
import com.training.dto.response.AuthResponse;
import com.training.entity.SysRole;
import com.training.entity.SysUser;
import com.training.entity.SysUserRole;
import com.training.exception.BusinessException;
import com.training.mapper.SysRoleMapper;
import com.training.mapper.SysUserMapper;
import com.training.mapper.SysUserRoleMapper;
import com.training.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());

        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        List<String> roles = getUserRoles(user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roles)
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());

        SysUser existingUser = sysUserMapper.selectByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        sysUserMapper.insert(user);

        SysRole studentRole = sysRoleMapper.selectByRoleCode("STUDENT");
        if (studentRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(studentRole.getId());
            sysUserRoleMapper.insert(userRole);
        }

        List<String> roles = getUserRoles(user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roles)
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("刷新令牌");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("无效的刷新令牌");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException("无效的刷新令牌");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        List<String> roles = getUserRoles(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .username(username)
                .roles(roles)
                .build();
    }

    private List<String> getUserRoles(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectByUserId(userId);
        return userRoles.stream()
                .map(ur -> sysRoleMapper.selectById(ur.getRoleId()))
                .filter(role -> role != null)
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }
}
