package com.HardwareProductSalesManagementSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.HardwareProductSalesManagementSystem.pojo.Role;
import com.HardwareProductSalesManagementSystem.service.RoleService;
import com.HardwareProductSalesManagementSystem.mapper.RoleMapper;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现（针对role表的数据库操作）
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}




