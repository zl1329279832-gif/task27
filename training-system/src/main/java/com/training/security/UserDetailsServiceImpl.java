package com.training.security;

import com.training.entity.SysRole;
import com.training.entity.SysUser;
import com.training.entity.SysUserRole;
import com.training.mapper.SysRoleMapper;
import com.training.mapper.SysUserMapper;
import com.training.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        List<SysUserRole> userRoles = sysUserRoleMapper.selectByUserId(user.getId());

        List<String> roleCodes = new ArrayList<>();
        for (SysUserRole userRole : userRoles) {
            SysRole role = sysRoleMapper.selectById(userRole.getRoleId());
            if (role != null) {
                roleCodes.add(role.getRoleCode());
            }
        }

        return new CustomUserDetails(user, roleCodes);
    }
}
