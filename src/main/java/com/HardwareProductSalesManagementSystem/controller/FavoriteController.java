package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.*;
import com.HardwareProductSalesManagementSystem.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收藏控制器
 * 基础路径：/api/v1/favorite
 */
@RestController
@RequestMapping("/api/v1/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductSpecService productSpecService;
    @Autowired
    private ProductImageService productImageService;

    /**
     * GET /api/v1/favorite/list - 获取当前用户收藏列表（含商品信息）
     */
    @GetMapping("/list")
    public APIRes list(Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "未登录");

        List<Favorite> favorites = favoriteService.list(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite fav : favorites) {
            Product product = productService.getById(fav.getProductId());
            if (product == null || product.getIsOnSale() != 1) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("favoriteId", fav.getFavoriteId());
            row.put("productId", product.getProductId());
            row.put("productName", product.getProductName());
            row.put("brand", product.getBrand());
            row.put("model", product.getModel());
            row.put("createTime", fav.getCreateTime());

            // 取第一个规格的价格和specId
            List<ProductSpec> specs = productSpecService.list(
                    new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, fav.getProductId())
                            .orderByAsc(ProductSpec::getSpecId));
            if (!specs.isEmpty()) {
                ProductSpec first = specs.get(0);
                row.put("firstSpecId", first.getSpecId());
                row.put("minPrice", first.getUnitPrice());
                row.put("unit", first.getUnit());
                row.put("stockQuantity", first.getStockQuantity());
                // 所有规格中最低价
                BigDecimal min = specs.stream()
                        .map(ProductSpec::getUnitPrice)
                        .filter(Objects::nonNull)
                        .min(java.math.BigDecimal::compareTo)
                        .orElse(first.getUnitPrice());
                row.put("minPrice", min);
            }

            // 取主图
            ProductImage mainImg = productImageService.getOne(
                    new LambdaQueryWrapper<ProductImage>()
                            .eq(ProductImage::getProductId, fav.getProductId())
                            .eq(ProductImage::getImageSort, 1)
                            .eq(ProductImage::getIsDeleted, 0)
                            .last("LIMIT 1"));
            row.put("mainImage", mainImg != null ? mainImg.getImageUrl() : null);

            result.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", result);
        data.put("total", result.size());
        return APIRes.ok("查询成功", data);
    }

    /**
     * POST /api/v1/favorite/toggle/{productId} - 收藏/取消收藏（切换）
     * 返回: { favorited: true/false }
     */
    @PostMapping("/toggle/{productId}")
    public APIRes toggle(@PathVariable Integer productId, Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.fail(401, "请先登录");
        if (productService.getById(productId) == null) return APIRes.fail(404, "商品不存在");

        Favorite existing = favoriteService.getOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId));

        if (existing != null) {
            favoriteService.removeById(existing.getFavoriteId());
            return APIRes.ok("已取消收藏", false);
        } else {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            fav.setCreateTime(new Date());
            favoriteService.save(fav);
            return APIRes.ok("收藏成功", true);
        }
    }

    /**
     * GET /api/v1/favorite/ids - 获取当前用户所有收藏的商品ID列表（用于批量判断状态）
     */
    @GetMapping("/ids")
    public APIRes ids(Authentication authentication) {
        Integer userId = getUserId(authentication);
        if (userId == null) return APIRes.ok("查询成功", Collections.emptyList());

        List<Integer> ids = favoriteService.list(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId))
                .stream().map(Favorite::getProductId).collect(Collectors.toList());
        return APIRes.ok("查询成功", ids);
    }

    private Integer getUserId(Authentication authentication) {
        if (authentication == null) return null;
        String username = authentication.getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? user.getUserId() : null;
    }
}
