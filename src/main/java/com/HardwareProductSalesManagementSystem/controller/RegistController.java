package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.impl.UserServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.Date;
import java.util.regex.Pattern;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@RestController
@RequestMapping("/user")
public class RegistController {
    @Resource
    private UserServiceImpl userService;
    @Resource
    private PasswordEncoder passwordEncoder;

    // 手机号正则（11位数字）
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @PostMapping("/regist")
    public APIRes regist(@RequestBody User user) {
        // 1. 校验必填参数
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return APIRes.fail(400, "用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return APIRes.fail(400, "密码不能为空且至少6位");
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return APIRes.fail(400, "手机号格式不正确（需11位有效数字）");
        }

        // 2. 校验用户名、手机号唯一性
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(User::getUsername, user.getUsername().trim());
        if (userService.getOne(userQueryWrapper) != null) {
            return APIRes.fail(400, "用户名已存在");
        }
        LambdaQueryWrapper<User> phoneQueryWrapper = new LambdaQueryWrapper<>();
        phoneQueryWrapper.eq(User::getPhone, user.getPhone());
        if (userService.getOne(phoneQueryWrapper) != null) {
            return APIRes.fail(400, "手机号已被注册");
        }

        // 3. 构造用户信息（核心修改：role_id默认设为0，status默认1）
        try {
            User newUser = new User();
            newUser.setUsername(user.getUsername().trim());
            newUser.setPassword(passwordEncoder.encode(user.getPassword())); // BCrypt加密
            newUser.setPhone(user.getPhone());
            newUser.setRoleId(User.DEFAULT_ROLE_ID); // 改为默认0
            newUser.setStatus(User.DEFAULT_STATUS); // 保持默认1
            newUser.setAvatar(User.DEFAULT_AVATAR); // 默认头像
            newUser.setCreateTime(new Date()); // 创建时间自动填充
            newUser.setRealName(user.getRealName()); // 允许为NULL
            userService.save(newUser);
            return APIRes.ok("注册成功", null);
        } catch (Exception e) {
            return APIRes.fail(500, "注册失败：" + e.getMessage());
        }
    }
}