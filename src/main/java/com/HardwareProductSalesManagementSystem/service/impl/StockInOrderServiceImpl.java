package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.StockInOrderMapper;
import com.HardwareProductSalesManagementSystem.pojo.StockInOrder;
import com.HardwareProductSalesManagementSystem.service.StockInOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class StockInOrderServiceImpl extends ServiceImpl<StockInOrderMapper, StockInOrder>
        implements StockInOrderService {
}
