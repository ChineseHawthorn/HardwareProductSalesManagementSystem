package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.commons.JwtUtil;
import com.HardwareProductSalesManagementSystem.pojo.Role;
import com.HardwareProductSalesManagementSystem.pojo.RolePermission;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.RolePermissionService;
import com.HardwareProductSalesManagementSystem.service.RoleService;
import com.HardwareProductSalesManagementSystem.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 用户管理控制器（API v1版本）
 * 基础路径：/api/v1/user
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserManagementController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Value("${upload.avatar.path}")
    private String avatarUploadPath;

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;

    // ======================== 认证接口 ========================

    /**
     * POST /api/v1/user/login - 用户登录
     * 请求体：{ username, password }
     * 返回：{ token, userId, username, roleId, roleName, realName, ... }
     */
    @PostMapping("/login")
    public APIRes login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return APIRes.fail(400, "用户名和密码不能为空");
        }
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User currentUser = userService.getOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, username));

            currentUser.setLastLoginTime(new Date());
            userService.updateById(currentUser);

            Role role = roleService.getById(currentUser.getRoleId());
            String token = jwtUtil.generateToken(userDetails, currentUser.getUserId());

            // 加载权限列表
            List<RolePermission> permissions = rolePermissionService.list(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, currentUser.getRoleId()));
            List<String> permCodes = new ArrayList<>();
            permissions.forEach(p -> permCodes.add(p.getPermissionCode()));

            String roleName = role != null ? role.getRoleName() : "未知角色";
            // 将中文角色名映射为前端使用的角色标识
            String roleCode;
            switch (roleName) {
                case "店主": roleCode = "ADMIN"; break;
                case "店员": roleCode = "STAFF"; break;
                case "企业客户": roleCode = "ENTERPRISE"; break;
                default: roleCode = "CONSUMER"; break;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userId", currentUser.getUserId());
            result.put("username", currentUser.getUsername());
            result.put("roleId", currentUser.getRoleId());
            result.put("roleName", roleName);
            result.put("role", roleCode);
            result.put("realName", currentUser.getRealName());
            result.put("phone", currentUser.getPhone());
            result.put("avatar", currentUser.getAvatar());
            result.put("permissions", permCodes);
            return APIRes.ok("登录成功", result);
        } catch (BadCredentialsException e) {
            return APIRes.fail(405, "账号或密码错误");
        } catch (Exception e) {
            return APIRes.fail(500, "登录失败：" + e.getMessage());
        }
    }

    /**
     * POST /api/v1/user/regist - 消费者注册
     * 请求体：{ username, password, phone, realName }
     */
    @PostMapping("/regist")
    public APIRes regist(@RequestBody User user) {
        if (StringUtils.isBlank(user.getUsername())) return APIRes.fail(400, "用户名不能为空");
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return APIRes.fail(400, "密码不能少于6位");
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            return APIRes.fail(400, "手机号格式不正确");
        }
        if (userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())) > 0) {
            return APIRes.fail(400, "用户名已存在");
        }
        if (userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, user.getPhone())) > 0) {
            return APIRes.fail(400, "手机号已被注册");
        }
        // 查找消费者角色
        Role consumerRole = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleName, "消费者"));
        user.setRoleId(consumerRole != null ? consumerRole.getRoleId() : 1);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setAvatar(User.DEFAULT_AVATAR);
        user.setCreateTime(new Date());
        userService.save(user);
        return APIRes.ok("注册成功");
    }

    /** GET /api/v1/user/info/{userId} - 获取用户信息 */
    @GetMapping("/info/{userId}")
    public APIRes userInfo(@PathVariable Integer userId) {
        User user = userService.getById(userId);
        if (user == null) return APIRes.fail(404, "用户不存在");
        user.setPassword(null); // 不返回密码
        return APIRes.ok("查询成功", user);
    }

    /**
     * GET /api/v1/user/info - 获取当前登录用户自身信息（从JWT解析）
     * 前端个人中心页面使用
     */
    @GetMapping("/info")
    public APIRes selfInfo(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return APIRes.fail(401, "未登录");
        String username = authentication.getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return APIRes.fail(404, "用户不存在");
        user.setPassword(null);
        return APIRes.ok("查询成功", user);
    }

    /** PUT /api/v1/user/update - 更新当前用户个人信息（前端个人中心使用） */
    @PutMapping("/update")
    public APIRes updateSelfInfo(@RequestBody Map<String, String> body,
                                 org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return APIRes.fail(401, "未登录");
        String username = authentication.getName();
        User current = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (current == null) return APIRes.fail(404, "用户不存在");
        User update = new User();
        update.setUserId(current.getUserId());
        // 支持修改昵称（用户名），需校验唯一性
        if (body.get("username") != null && !body.get("username").trim().isEmpty()) {
            String newUsername = body.get("username").trim();
            long count = userService.count(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, newUsername)
                    .ne(User::getUserId, current.getUserId()));
            if (count > 0) return APIRes.fail(400, "昵称已被使用");
            update.setUsername(newUsername);
        }
        if (body.get("realName") != null) update.setRealName(body.get("realName"));
        if (body.get("phone") != null) {
            String phone = body.get("phone").trim();
            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                return APIRes.fail(400, "手机号格式不正确");
            }
            if (!phone.isEmpty()) {
                long phoneCount = userService.count(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .ne(User::getUserId, current.getUserId()));
                if (phoneCount > 0) return APIRes.fail(400, "手机号已被使用");
            }
            update.setPhone(phone.isEmpty() ? null : phone);
        }
        if (body.get("avatar") != null) update.setAvatar(body.get("avatar"));
        userService.updateById(update);
        // 返回更新后的用户信息（含新用户名，方便前端刷新localStorage）
        User updated = userService.getById(current.getUserId());
        updated.setPassword(null);
        return APIRes.ok("修改成功", updated);
    }

    /** POST /api/v1/user/avatar/upload - 上传头像 */
    @PostMapping("/avatar/upload")
    public APIRes uploadAvatar(@RequestParam("file") MultipartFile file,
                               org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return APIRes.fail(401, "未登录");
        if (file.isEmpty()) return APIRes.fail(400, "文件不能为空");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return APIRes.fail(400, "仅支持上传图片文件");
        }
        try {
            String ext = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + ext;
            File dest = new File(avatarUploadPath + filename);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);
            String avatarUrl = "/images/avatar/" + filename;

            String username = authentication.getName();
            User current = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (current == null) return APIRes.fail(404, "用户不存在");
            User update = new User();
            update.setUserId(current.getUserId());
            update.setAvatar(avatarUrl);
            userService.updateById(update);
            return APIRes.ok("头像上传成功", avatarUrl);
        } catch (Exception e) {
            return APIRes.fail(500, "上传失败：" + e.getMessage());
        }
    }

    /** PUT /api/v1/user/password - 修改当前用户密码 */
    @PutMapping("/password")
    public APIRes changePassword(@RequestBody Map<String, String> body,
                                 org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return APIRes.fail(401, "未登录");
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (StringUtils.isBlank(oldPwd) || StringUtils.isBlank(newPwd)) {
            return APIRes.fail(400, "原密码和新密码不能为空");
        }
        if (newPwd.length() < 6) return APIRes.fail(400, "新密码不能少于6位");

        String username = authentication.getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return APIRes.fail(404, "用户不存在");
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            return APIRes.fail(400, "原密码不正确");
        }
        User update = new User();
        update.setUserId(user.getUserId());
        update.setPassword(passwordEncoder.encode(newPwd));
        userService.updateById(update);
        return APIRes.ok("密码修改成功");
    }

    /** PUT /api/v1/user/info/update - 更新个人信息（兼容旧接口） */
    @PutMapping("/info/update")
    public APIRes updateInfo(@RequestBody User user) {
        if (user.getUserId() == null) return APIRes.fail(400, "用户ID不能为空");
        User update = new User();
        update.setUserId(user.getUserId());
        update.setRealName(user.getRealName());
        update.setAvatar(user.getAvatar());
        // 不允许自行修改角色和状态
        userService.updateById(update);
        return APIRes.ok("修改成功");
    }

    // ======================== 消费者管理（店主） ========================

    /** GET /api/v1/user/consumer/list - 消费者列表（店主） */
    @GetMapping("/consumer/list")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes consumerList(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer status) {
        // 查找消费者角色ID
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleName, "消费者");
        Role consumerRole = roleService.getOne(roleWrapper);
        if (consumerRole == null) return APIRes.fail(500, "消费者角色未配置");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRoleId, consumerRole.getRoleId());
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (status != null) wrapper.eq(User::getStatus, status);
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = userService.page(new Page<>(pageNum, pageSize), wrapper);
        // 清除密码字段
        page.getRecords().forEach(u -> u.setPassword(null));
        return APIRes.ok("查询成功", page);
    }

    // ======================== 店员管理（店主） ========================

    /** GET /api/v1/user/staff/list - 店员列表（店主） */
    @GetMapping("/staff/list")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes staffList(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer status) {
        Role staffRole = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleName, "店员"));
        if (staffRole == null) return APIRes.fail(500, "店员角色未配置");

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRoleId, staffRole.getRoleId());
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword));
        }
        if (status != null) wrapper.eq(User::getStatus, status);
        Page<User> page = userService.page(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return APIRes.ok("查询成功", page);
    }

    /** POST /api/v1/user/staff/add - 新增店员账号（店主） */
    @PostMapping("/staff/add")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes staffAdd(@RequestBody User user) {
        if (StringUtils.isBlank(user.getUsername())) return APIRes.fail(400, "用户名不能为空");
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return APIRes.fail(400, "密码不能少于6位");
        }
        if (userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())) > 0) {
            return APIRes.fail(400, "用户名已存在");
        }
        Role staffRole = roleService.getOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleName, "店员"));
        user.setRoleId(staffRole != null ? staffRole.getRoleId() : 2);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setAvatar(User.DEFAULT_AVATAR);
        user.setCreateTime(new Date());
        userService.save(user);
        user.setPassword(null);
        return APIRes.ok("店员账号创建成功", user);
    }

    /** PUT /api/v1/user/staff/status/{id} - 启用/禁用店员账号（店主） */
    @PutMapping("/staff/status/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes staffStatus(@PathVariable Integer id, @RequestParam Integer status) {
        User u = new User();
        u.setUserId(id);
        u.setStatus(status);
        userService.updateById(u);
        return APIRes.ok(status == 1 ? "账号已启用" : "账号已禁用");
    }

    /** PUT /api/v1/user/staff/reset-pwd/{id} - 重置店员密码（店主） */
    @PutMapping("/staff/reset-pwd/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes staffResetPwd(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 6) return APIRes.fail(400, "新密码不能少于6位");
        User u = new User();
        u.setUserId(id);
        u.setPassword(passwordEncoder.encode(newPwd));
        userService.updateById(u);
        return APIRes.ok("密码重置成功");
    }

    /** PUT /api/v1/user/admin/edit/{id} - 店主编辑任意用户信息（账号、密码、手机号） */
    @PutMapping("/admin/edit/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes adminEditUser(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        User existing = userService.getById(id);
        if (existing == null) return APIRes.fail(404, "用户不存在");

        User update = new User();
        update.setUserId(id);
        boolean changed = false;

        // 修改用户名
        if (body.containsKey("username") && StringUtils.isNotBlank(body.get("username"))) {
            String newUsername = body.get("username").trim();
            long count = userService.count(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, newUsername)
                    .ne(User::getUserId, id));
            if (count > 0) return APIRes.fail(400, "用户名已存在");
            update.setUsername(newUsername);
            changed = true;
        }
        // 修改密码
        if (body.containsKey("password") && StringUtils.isNotBlank(body.get("password"))) {
            String newPwd = body.get("password");
            if (newPwd.length() < 6) return APIRes.fail(400, "密码不能少于6位");
            update.setPassword(passwordEncoder.encode(newPwd));
            changed = true;
        }
        // 修改手机号
        if (body.containsKey("phone")) {
            String phone = body.get("phone").trim();
            if (!phone.isEmpty()) {
                if (!PHONE_PATTERN.matcher(phone).matches()) return APIRes.fail(400, "手机号格式不正确");
                long phoneCount = userService.count(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .ne(User::getUserId, id));
                if (phoneCount > 0) return APIRes.fail(400, "手机号已被使用");
            }
            update.setPhone(phone.isEmpty() ? null : phone);
            changed = true;
        }

        if (!changed) return APIRes.fail(400, "未提供任何修改内容");
        userService.updateById(update);
        User updated = userService.getById(id);
        updated.setPassword(null);
        return APIRes.ok("修改成功", updated);
    }

    /** PUT /api/v1/user/admin/reset-avatar/{id} - 店主重置用户头像为默认 */
    @PutMapping("/admin/reset-avatar/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes adminResetAvatar(@PathVariable Integer id) {
        User existing = userService.getById(id);
        if (existing == null) return APIRes.fail(404, "用户不存在");
        User update = new User();
        update.setUserId(id);
        update.setAvatar(User.DEFAULT_AVATAR);
        userService.updateById(update);
        return APIRes.ok("头像已重置");
    }

    /** PUT /api/v1/user/consumer/status/{id} - 启用/禁用消费者账号（店主） */
    @PutMapping("/consumer/status/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes consumerStatus(@PathVariable Integer id, @RequestParam Integer status) {
        User u = new User();
        u.setUserId(id);
        u.setStatus(status);
        userService.updateById(u);
        return APIRes.ok(status == 1 ? "账号已启用" : "账号已禁用");
    }

    // ======================== 角色权限管理（店主） ========================

    /** GET /api/v1/user/role/list - 角色列表（店主） */
    @GetMapping("/role/list")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes roleList() {
        List<Role> roles = roleService.list();
        return APIRes.ok("查询成功", roles);
    }

    /** GET /api/v1/user/role/permission/{roleId} - 查询角色权限（店主） */
    @GetMapping("/role/permission/{roleId}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes rolePermissionList(@PathVariable Integer roleId) {
        List<RolePermission> permissions = rolePermissionService.list(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        return APIRes.ok("查询成功", permissions);
    }

    /** PUT /api/v1/user/role/permission/{roleId} - 更新角色权限（店主） */
    @PutMapping("/role/permission/{roleId}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes updateRolePermission(@PathVariable Integer roleId,
                                       @RequestBody List<RolePermission> permissions) {
        // 删除旧权限，插入新权限
        rolePermissionService.remove(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (permissions != null && !permissions.isEmpty()) {
            permissions.forEach(p -> {
                p.setRoleId(roleId);
                p.setCreateTime(new Date());
            });
            rolePermissionService.saveBatch(permissions);
        }
        return APIRes.ok("权限更新成功");
    }
}
