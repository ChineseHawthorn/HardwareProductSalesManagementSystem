package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.CartMapper;
import com.HardwareProductSalesManagementSystem.pojo.Cart;
import com.HardwareProductSalesManagementSystem.service.CartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart>
        implements CartService {
}
