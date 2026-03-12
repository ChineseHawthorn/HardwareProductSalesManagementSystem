package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.OrderDetailMapper;
import com.HardwareProductSalesManagementSystem.pojo.OrderDetail;
import com.HardwareProductSalesManagementSystem.service.OrderDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail>
        implements OrderDetailService {
}
