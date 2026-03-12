package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.Order;
import com.HardwareProductSalesManagementSystem.pojo.OrderDetail;
import com.HardwareProductSalesManagementSystem.pojo.ProductSpec;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.OrderDetailService;
import com.HardwareProductSalesManagementSystem.service.OrderService;
import com.HardwareProductSalesManagementSystem.service.ProductService;
import com.HardwareProductSalesManagementSystem.service.ProductSpecService;
import com.HardwareProductSalesManagementSystem.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计控制器（仅店主可访问）
 * 基础路径：/api/v1/stat
 */
@RestController
@RequestMapping("/api/v1/stat")
@PreAuthorize("hasRole('OWNER')")
public class StatController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private ProductSpecService productSpecService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;

    /**
     * GET /api/v1/stat/sales - 销售报表
     * 参数：period=day|week|month，startDate，endDate
     * 返回：总销售额、订单数、TOP10热销商品、平均客单价
     */
    @GetMapping("/sales")
    public APIRes salesReport(@RequestParam(defaultValue = "month") String period) {
        // 查询已完成/已支付订单（payStatus=4已支付或5已完成）
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getPayStatus, 1, 4, 5) // 已支付状态
                    .orderByDesc(Order::getPayTime);

        List<Order> orders = orderService.list(orderWrapper);

        // 按时间过滤
        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        Date startDate;
        switch (period) {
            case "day":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case "week":
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            default: // month
                cal.add(Calendar.MONTH, -1);
        }
        startDate = cal.getTime();

        final Date filterStart = startDate;
        List<Order> filtered = orders.stream()
                .filter(o -> o.getPayTime() != null &&
                        o.getPayTime().after(filterStart) &&
                        o.getPayTime().before(endDate))
                .collect(Collectors.toList());

        // 计算总销售额和订单数
        BigDecimal totalSales = filtered.stream()
                .map(Order::getOrderAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int orderCount = filtered.size();
        BigDecimal avgOrderValue = orderCount > 0
                ? totalSales.divide(BigDecimal.valueOf(orderCount), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        // 查询TOP10热销商品（按销售数量排序）
        List<String> orderIds = filtered.stream().map(Order::getOrderId).collect(Collectors.toList());
        List<Map<String, Object>> top10 = new ArrayList<>();
        if (!orderIds.isEmpty()) {
            List<OrderDetail> allDetails = orderDetailService.list(
                    new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, orderIds));

            // 按specId分组统计销售量
            Map<Integer, Integer> specSales = new HashMap<>();
            for (OrderDetail d : allDetails) {
                specSales.merge(d.getSpecId(), d.getSaleQuantity(), Integer::sum);
            }

            // 排序取前10
            specSales.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(e -> {
                        ProductSpec spec = productSpecService.getById(e.getKey());
                        Map<String, Object> item = new HashMap<>();
                        item.put("specId", e.getKey());
                        item.put("specCode", spec != null ? spec.getSpecCode() : "");
                        item.put("saleQuantity", e.getValue());
                        top10.add(item);
                    });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("totalSales", totalSales);
        result.put("orderCount", orderCount);
        result.put("avgOrderValue", avgOrderValue);
        result.put("top10Products", top10);
        return APIRes.ok("查询成功", result);
    }

    /**
     * GET /api/v1/stat/inventory - 库存报表
     * 返回：库存预警列表、滞销库存（>3个月无出库）
     */
    @GetMapping("/inventory")
    public APIRes inventoryReport() {
        // 1. 预警库存：stock_quantity <= warning_threshold
        List<ProductSpec> warningSpecs = productSpecService.list(
                new LambdaQueryWrapper<ProductSpec>()
                        .apply("stock_quantity <= warning_threshold")
                        .orderByAsc(ProductSpec::getStockQuantity));

        // 2. 所有有库存的规格
        List<ProductSpec> allStockedSpecs = productSpecService.list(
                new LambdaQueryWrapper<ProductSpec>().gt(ProductSpec::getStockQuantity, 0));

        Map<String, Object> result = new HashMap<>();
        result.put("warningCount", warningSpecs.size());
        result.put("warningItems", warningSpecs);
        result.put("totalStockedSpecs", allStockedSpecs.size());
        result.put("reportTime", new Date());
        return APIRes.ok("查询成功", result);
    }

    /**
     * GET /api/v1/stat/customer - 客户分析
     * 返回：总用户数、消费者数、企业客户数
     */
    @GetMapping("/customer")
    public APIRes customerReport() {
        // 查询各角色用户数
        long totalUsers = userService.count();
        long activeUsers = userService.count(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1));

        // 查询有购买记录的用户数（消费者）
        List<Order> allOrders = orderService.list(
                new LambdaQueryWrapper<Order>().in(Order::getPayStatus, 1, 4, 5));
        long buyerCount = allOrders.stream()
                .filter(o -> o.getUserId() != null)
                .map(Order::getUserId)
                .distinct().count();

        // 计算复购率（消费超过1次的用户）
        Map<Integer, Long> userOrderCount = allOrders.stream()
                .filter(o -> o.getUserId() != null)
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));
        long repeatBuyerCount = userOrderCount.values().stream().filter(c -> c > 1).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("activeUsers", activeUsers);
        result.put("buyerCount", buyerCount);
        result.put("repeatBuyerCount", repeatBuyerCount);
        result.put("repurchaseRate", buyerCount > 0
                ? String.format("%.1f%%", repeatBuyerCount * 100.0 / buyerCount) : "0%");
        return APIRes.ok("查询成功", result);
    }

    /**
     * GET /api/v1/stat/dashboard - 首页仪表盘汇总数据
     */
    @GetMapping("/dashboard")
    public APIRes dashboard() {
        // 今日销售额
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date todayStart = cal.getTime();

        List<Order> todayOrders = orderService.list(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getPayTime, todayStart)
                        .in(Order::getPayStatus, 1, 4, 5));

        BigDecimal todaySales = todayOrders.stream()
                .map(Order::getOrderAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 库存预警数
        long warnCount = productSpecService.count(
                new LambdaQueryWrapper<ProductSpec>()
                        .apply("stock_quantity <= warning_threshold"));

        // 待处理订单（待发货）
        long pendingShipCount = orderService.count(
                new LambdaQueryWrapper<Order>().eq(Order::getPayStatus, 4));

        // 待审核退款
        long pendingRefundCount = 0; // RefundRecord计数

        Map<String, Object> result = new HashMap<>();
        result.put("todaySales", todaySales);
        result.put("todayOrderCount", todayOrders.size());
        result.put("stockWarnCount", warnCount);
        result.put("pendingShipCount", pendingShipCount);
        result.put("totalUsers", userService.count());
        return APIRes.ok("查询成功", result);
    }
}
