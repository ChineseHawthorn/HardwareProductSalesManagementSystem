package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.StockOutOrderMapper;
import com.HardwareProductSalesManagementSystem.pojo.StockOutOrder;
import com.HardwareProductSalesManagementSystem.service.StockOutOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class StockOutOrderServiceImpl extends ServiceImpl<StockOutOrderMapper, StockOutOrder>
        implements StockOutOrderService {
}
