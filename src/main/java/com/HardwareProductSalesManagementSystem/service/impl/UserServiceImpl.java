package com.HardwareProductSalesManagementSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.UserService;
import com.HardwareProductSalesManagementSystem.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 19167
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-01-22 21:02:45
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




