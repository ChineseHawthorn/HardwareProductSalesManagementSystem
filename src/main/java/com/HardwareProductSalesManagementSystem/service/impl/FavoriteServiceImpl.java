package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.FavoriteMapper;
import com.HardwareProductSalesManagementSystem.pojo.Favorite;
import com.HardwareProductSalesManagementSystem.service.FavoriteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite>
        implements FavoriteService {
}
