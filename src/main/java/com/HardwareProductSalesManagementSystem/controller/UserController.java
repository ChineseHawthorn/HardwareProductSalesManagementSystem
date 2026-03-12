package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息控制器（获取头像接口）
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserServiceImpl userService;

    // 根据用户ID获取最新头像（对齐数据表avatar字段）
    @GetMapping("/avatar/{userId}")
    public APIRes getUserAvatar(@PathVariable Integer userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return APIRes.fail(404, "用户不存在");
            }
            // 若头像为NULL，返回默认头像地址
            String avatar = user.getAvatar() == null ? User.DEFAULT_AVATAR : user.getAvatar();
            return APIRes.ok("获取头像成功", avatar);
        } catch (Exception e) {
            return APIRes.fail(500, "获取头像失败：" + e.getMessage());
        }
    }

    // 根据用户ID获取用户信息（含用户名、头像）
    @GetMapping("/info/{userId}")
    public APIRes getUserInfo(@PathVariable Integer userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return APIRes.fail(404, "用户不存在");
            }
            // 封装需要的信息（用户名、头像）
            UserInfoVO userInfoVO = new UserInfoVO();
            userInfoVO.setUsername(user.getUsername());
            userInfoVO.setAvatar(user.getAvatar() == null ? User.DEFAULT_AVATAR : user.getAvatar());
            return APIRes.ok("获取用户信息成功", userInfoVO);
        } catch (Exception e) {
            return APIRes.fail(500, "获取用户信息失败：" + e.getMessage());
        }
    }

    // 内部VO类，用于返回指定用户信息
    private static class UserInfoVO {
        private String username;
        private String avatar;

        // Getter & Setter
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }

}