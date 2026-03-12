package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.StockLogMapper;
import com.HardwareProductSalesManagementSystem.pojo.StockLog;
import com.HardwareProductSalesManagementSystem.service.StockLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class StockLogServiceImpl extends ServiceImpl<StockLogMapper, StockLog>
        implements StockLogService {
}
