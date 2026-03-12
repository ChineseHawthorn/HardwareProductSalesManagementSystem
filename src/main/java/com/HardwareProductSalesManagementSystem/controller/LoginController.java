package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.commons.JwtUtil;
import com.HardwareProductSalesManagementSystem.pojo.Role;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.impl.RoleServiceImpl;
import com.HardwareProductSalesManagementSystem.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户登录控制器（补充最后登录时间更新、角色信息返回）
 */
@RestController
@RequestMapping("/user")
public class LoginController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private RoleServiceImpl roleService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public APIRes login(@RequestBody User loginUser) {
        try {
            // 1. 构建认证Token（用户名+密码）
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginUser.getUsername(), loginUser.getPassword());

            // 2. 调用AuthenticationManager完成认证
            Authentication authentication = authenticationManager.authenticate(authToken);

            // 3. 将认证信息存入SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 4. 查询当前登录用户的完整信息
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, userDetails.getUsername());
            User currentUser = userService.getOne(queryWrapper);

            // 5. 更新最后登录时间
            currentUser.setLastLoginTime(new Date());
            userService.updateById(currentUser);

            // 6. 查询角色信息（关联role表）
            Role role = roleService.getById(currentUser.getRoleId());

            // 7. 生成JWT令牌（包含用户ID）
            String token = jwtUtil.generateToken(userDetails, currentUser.getUserId());

            // 8. 构造返回数据（补充角色名称、真实姓名、账号状态）
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("token", token);
            resultData.put("userId", currentUser.getUserId());
            resultData.put("username", currentUser.getUsername());
            resultData.put("roleId", currentUser.getRoleId());
            resultData.put("roleName", role != null ? role.getRoleName() : "未知角色");
            resultData.put("realName", currentUser.getRealName());
            resultData.put("phone", currentUser.getPhone());
            resultData.put("avatar", currentUser.getAvatar());
            resultData.put("status", currentUser.getStatus() == 1 ? "正常" : "禁用");

            // 9. 返回成功响应
            return APIRes.ok("登录成功", resultData);
        } catch (UsernameNotFoundException e) {
            return APIRes.fail(405, "账号不存在");
        } catch (BadCredentialsException e) {
            return APIRes.fail(405, "账号或密码错误");
        } catch (Exception e) {
            return APIRes.fail(500, "登录失败：" + e.getMessage());
        }
    }
}