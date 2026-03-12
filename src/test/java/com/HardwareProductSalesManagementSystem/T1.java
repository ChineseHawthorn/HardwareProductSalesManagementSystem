package com.HardwareProductSalesManagementSystem;

import com.HardwareProductSalesManagementSystem.mapper.UserMapper;
import com.HardwareProductSalesManagementSystem.service.UserService;
import com.HardwareProductSalesManagementSystem.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author 姜姜
 * @title T1
 * @date 2026/1/13 10:51
 * @description TODO
 */
@SpringBootTest
public class T1 {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UserService userService;

    @Test
    public void run(){
        /* 查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username","user000");
        queryWrapper.like("username","user000");
        List<User> list = userService.list(queryWrapper);

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUsername,"user000");
        User user = userService.getOne(lambdaQueryWrapper);
        */

        /* 保存
        User user = new User();
        user.setId(1);
        user.setUsername("admin");
        boolean save = userService.save(user);
        */

        /*  更新
        User user = new User();
        user.setPassword("123456");
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUsername,"user000");
        boolean update = userService.update(user,lambdaQueryWrapper);
        */

        /* 删除
        userService.remove(new QueryWrapper<User>().eq("username", "admin"));
        */

        /* 分页查询
        IPage<User> users = userService.getUsers(1, 10, null);
        System.out.println(users.getCurrent()); //当前页码
        System.out.println(users.getPages());   //总页数
        System.out.println(users.getRecords()); //当前页的记录列表
        System.out.println(users.getSize());    //每页记录数
        System.out.println(users.getTotal());   //总记录数
        System.out.println(users.maxLimit());   //分页插件的最大限制值
        System.out.println(users.offset());     //当前页的偏移量
        */

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        //加密
        String encodedPwd = encoder.encode("372729");
        System.out.println("加密后：" + encodedPwd);

        //比对
        boolean isMatch = encoder.matches("372729", encodedPwd);
        System.out.println("比对结果：" + isMatch);





    }
}
