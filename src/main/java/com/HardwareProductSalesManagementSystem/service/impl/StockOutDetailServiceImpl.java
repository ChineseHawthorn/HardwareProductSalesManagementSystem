package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.StockOutDetailMapper;
import com.HardwareProductSalesManagementSystem.pojo.StockOutDetail;
import com.HardwareProductSalesManagementSystem.service.StockOutDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class StockOutDetailServiceImpl extends ServiceImpl<StockOutDetailMapper, StockOutDetail>
        implements StockOutDetailService {
}
