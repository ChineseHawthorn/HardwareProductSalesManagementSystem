package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.StockInDetailMapper;
import com.HardwareProductSalesManagementSystem.pojo.StockInDetail;
import com.HardwareProductSalesManagementSystem.service.StockInDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class StockInDetailServiceImpl extends ServiceImpl<StockInDetailMapper, StockInDetail>
        implements StockInDetailService {
}
