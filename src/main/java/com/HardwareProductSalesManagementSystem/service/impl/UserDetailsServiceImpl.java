package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.pojo.Role;
import com.HardwareProductSalesManagementSystem.pojo.RolePermission;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security用户详情服务（加载角色权限用于@PreAuthorize鉴权）
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private RoleServiceImpl roleService;

    @Resource
    private RolePermissionServiceImpl rolePermissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUsername, username);
        User user = userService.getOne(userWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("账号已被禁用");
        }

        // 2. 查询角色
        Role role = roleService.getById(user.getRoleId());

        // 3. 构建权限集合
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 3.1 添加角色权限（ROLE_前缀，用于hasRole()校验）
        if (role != null) {
            String roleAuthority = mapRoleNameToAuthority(role.getRoleName());
            authorities.add(new SimpleGrantedAuthority(roleAuthority));
        }

        // 3.2 加载该角色的细粒度权限码（用于hasAuthority()校验）
        if (user.getRoleId() != null) {
            LambdaQueryWrapper<RolePermission> permWrapper = new LambdaQueryWrapper<>();
            permWrapper.eq(RolePermission::getRoleId, user.getRoleId());
            List<RolePermission> permissions = rolePermissionService.list(permWrapper);
            for (RolePermission perm : permissions) {
                authorities.add(new SimpleGrantedAuthority(perm.getPermissionCode()));
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    /** 将角色名称映射为Spring Security的ROLE_前缀权限 */
    private String mapRoleNameToAuthority(String roleName) {
        if ("店主".equals(roleName)) return "ROLE_OWNER";
        if ("店员".equals(roleName)) return "ROLE_STAFF";
        if ("消费者".equals(roleName)) return "ROLE_CONSUMER";
        if ("企业客户".equals(roleName)) return "ROLE_ENTERPRISE";
        return "ROLE_CONSUMER";
    }
}
