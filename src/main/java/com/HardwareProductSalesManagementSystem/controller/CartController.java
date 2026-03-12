package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.*;
import com.HardwareProductSalesManagementSystem.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 购物车控制器
 * 基础路径：/api/v1/cart
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    @Autowired
    private CartService cartService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductSpecService productSpecService;
    @Autowired
    private ProductService productService;

    /**
     * GET /api/v1/cart/list - 获取当前用户的购物车列表（含商品信息）
     */
    @GetMapping("/list")
    public APIRes list(Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "未登录");

        List<Cart> cartItems = cartService.list(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreateTime));

        // 组装详情：规格信息 + 商品信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (Cart item : cartItems) {
            Map<String, Object> row = new HashMap<>();
            row.put("cartId", item.getCartId());
            row.put("userId", item.getUserId());
            row.put("specId", item.getSpecId());
            row.put("quantity", item.getQuantity());
            row.put("createTime", item.getCreateTime());

            // 查规格
            ProductSpec spec = productSpecService.getById(item.getSpecId());
            if (spec != null) {
                row.put("specCode", spec.getSpecCode());
                row.put("material", spec.getMaterial());
                row.put("size", spec.getSize());
                row.put("unit", spec.getUnit());
                row.put("unitPrice", spec.getUnitPrice());
                row.put("stockQuantity", spec.getStockQuantity());

                // 查商品
                Product product = productService.getById(spec.getProductId());
                if (product != null) {
                    row.put("productId", product.getProductId());
                    row.put("productName", product.getProductName());
                    row.put("brand", product.getBrand());
                    row.put("model", product.getModel());
                    row.put("isOnSale", product.getIsOnSale());
                }
            }
            result.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", result);
        data.put("total", result.size());
        return APIRes.ok("查询成功", data);
    }

    /**
     * POST /api/v1/cart/add - 加入购物车
     * 请求体：{ specId, quantity }
     * 若该规格已在购物车，则累加数量
     */
    @PostMapping("/add")
    public APIRes add(@RequestBody Map<String, Object> body, Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "请先登录");

        Integer specId = Integer.valueOf(body.get("specId").toString());
        Integer quantity = body.get("quantity") != null
                ? Integer.valueOf(body.get("quantity").toString()) : 1;
        if (quantity <= 0) return APIRes.fail(400, "数量必须大于0");

        // 校验规格是否存在及库存
        ProductSpec spec = productSpecService.getById(specId);
        if (spec == null) return APIRes.fail(404, "商品规格不存在");
        if (spec.getStockQuantity() <= 0) return APIRes.fail(400, "库存不足，无法加入购物车");

        // 检查购物车中是否已有该规格
        Cart existing = cartService.getOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getSpecId, specId));

        if (existing != null) {
            // 已存在：累加数量，但不超过库存
            int newQty = Math.min(existing.getQuantity() + quantity, spec.getStockQuantity());
            cartService.update(new LambdaUpdateWrapper<Cart>()
                    .eq(Cart::getCartId, existing.getCartId())
                    .set(Cart::getQuantity, newQty)
                    .set(Cart::getUpdateTime, new Date()));
        } else {
            // 新增
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setSpecId(specId);
            cart.setQuantity(Math.min(quantity, spec.getStockQuantity()));
            cart.setCreateTime(new Date());
            cart.setUpdateTime(new Date());
            cartService.save(cart);
        }
        return APIRes.ok("已加入购物车");
    }

    /**
     * PUT /api/v1/cart/update - 修改购物车商品数量
     * 请求体：{ cartId, quantity }
     */
    @PutMapping("/update")
    public APIRes update(@RequestBody Map<String, Object> body, Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "请先登录");

        Integer cartId = Integer.valueOf(body.get("cartId").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        if (quantity <= 0) return APIRes.fail(400, "数量必须大于0");

        Cart cart = cartService.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            return APIRes.fail(404, "购物车记录不存在");
        }

        // 检查库存
        ProductSpec spec = productSpecService.getById(cart.getSpecId());
        if (spec != null && quantity > spec.getStockQuantity()) {
            return APIRes.fail(400, "库存不足，最多可购买 " + spec.getStockQuantity() + " 件");
        }

        cartService.update(new LambdaUpdateWrapper<Cart>()
                .eq(Cart::getCartId, cartId)
                .set(Cart::getQuantity, quantity)
                .set(Cart::getUpdateTime, new Date()));
        return APIRes.ok("数量已更新");
    }

    /**
     * DELETE /api/v1/cart/remove/{cartId} - 删除购物车单个商品
     */
    @DeleteMapping("/remove/{cartId}")
    public APIRes remove(@PathVariable Integer cartId, Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "请先登录");

        Cart cart = cartService.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            return APIRes.fail(404, "购物车记录不存在");
        }
        cartService.removeById(cartId);
        return APIRes.ok("已移除");
    }

    /**
     * DELETE /api/v1/cart/clear - 清空当前用户购物车
     */
    @DeleteMapping("/clear")
    public APIRes clear(Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "请先登录");

        cartService.remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return APIRes.ok("购物车已清空");
    }

    /**
     * GET /api/v1/cart/count - 获取当前用户购物车商品种数
     */
    @GetMapping("/count")
    public APIRes count(Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.ok("查询成功", 0);

        long cnt = cartService.count(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return APIRes.ok("查询成功", cnt);
    }

    // ---- 工具方法：从JWT Authentication中获取userId ----
    private Integer getUserId(Authentication authentication) {
        if (authentication == null) return null;
        String username = authentication.getName();
        User user = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? user.getUserId() : null;
    }
}
