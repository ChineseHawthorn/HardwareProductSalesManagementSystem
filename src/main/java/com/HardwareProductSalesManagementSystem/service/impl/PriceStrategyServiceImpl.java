package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.PriceStrategyMapper;
import com.HardwareProductSalesManagementSystem.pojo.PriceStrategy;
import com.HardwareProductSalesManagementSystem.service.PriceStrategyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PriceStrategyServiceImpl extends ServiceImpl<PriceStrategyMapper, PriceStrategy>
        implements PriceStrategyService {
}
