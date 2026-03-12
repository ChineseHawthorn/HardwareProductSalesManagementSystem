package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.RolePermissionMapper;
import com.HardwareProductSalesManagementSystem.pojo.RolePermission;
import com.HardwareProductSalesManagementSystem.service.RolePermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>
        implements RolePermissionService {
}
