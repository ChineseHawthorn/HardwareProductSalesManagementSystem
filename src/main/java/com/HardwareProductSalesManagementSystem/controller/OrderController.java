package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.*;
import com.HardwareProductSalesManagementSystem.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 订单管理控制器
 * 基础路径：/api/v1/order
 */
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private PaymentRecordService paymentRecordService;
    @Autowired
    private RefundRecordService refundRecordService;
    @Autowired
    private ProductSpecService productSpecService;
    @Autowired
    private StockOutOrderService stockOutOrderService;
    @Autowired
    private StockOutDetailService stockOutDetailService;
    @Autowired
    private StockLogService stockLogService;

    // ======================== 创建订单 ========================

    /**
     * POST /api/v1/order/create - 创建订单
     * 请求体：{
     *   userId, orderType(1=线上,2=线下), tradeType(1=批发,2=零售),
     *   receiverName, receiverPhone, receiverAddr,
     *   remark,
     *   items:[{specId, saleQuantity, salePrice}]
     * }
     */
    @PostMapping("/create")
    @Transactional
    public APIRes create(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) {
            return APIRes.fail(400, "订单明细不能为空");
        }

        // 校验库存
        for (Map<String, Object> item : items) {
            Integer specId = Integer.valueOf(item.get("specId").toString());
            Integer qty = Integer.valueOf(item.get("saleQuantity").toString());
            ProductSpec spec = productSpecService.getById(specId);
            if (spec == null) return APIRes.fail(400, "规格不存在：specId=" + specId);
            if (spec.getStockQuantity() < qty) {
                return APIRes.fail(400, "库存不足：" + spec.getSpecCode() +
                        "，当前库存：" + spec.getStockQuantity());
            }
        }

        String orderId = generateOrderId();
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(body.get("userId") != null ?
                Integer.valueOf(body.get("userId").toString()) : null);
        order.setOrderType(Integer.valueOf(body.get("orderType").toString()));
        order.setTradeType(Integer.valueOf(body.get("tradeType").toString()));
        order.setReceiverName((String) body.get("receiverName"));
        order.setReceiverPhone((String) body.get("receiverPhone"));
        order.setReceiverAddr((String) body.get("receiverAddr"));
        order.setRemark((String) body.get("remark"));
        order.setPayStatus(0); // 待支付
        order.setCreateTime(new Date());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> detailList = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Integer specId = Integer.valueOf(item.get("specId").toString());
            Integer qty = Integer.valueOf(item.get("saleQuantity").toString());
            BigDecimal price = new BigDecimal(item.get("salePrice").toString());
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);

            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orderId);
            detail.setSpecId(specId);
            detail.setSaleQuantity(qty);
            detail.setSalePrice(price);
            detail.setSubtotal(subtotal);
            detail.setIsRefunded(0);
            detailList.add(detail);
        }

        order.setOrderAmount(totalAmount);
        orderService.save(order);
        orderDetailService.saveBatch(detailList);

        return APIRes.ok("订单创建成功", order);
    }

    // ======================== 查询订单 ========================

    /**
     * GET /api/v1/order/list - 订单列表
     * 店主查所有，店员查自己处理的，消费者查自己的
     */
    @GetMapping("/list")
    public APIRes list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) Integer userId,
                       @RequestParam(required = false) Integer payStatus,
                       @RequestParam(required = false) Integer orderType) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Order::getUserId, userId);
        if (payStatus != null) wrapper.eq(Order::getPayStatus, payStatus);
        if (orderType != null) wrapper.eq(Order::getOrderType, orderType);
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = orderService.page(new Page<>(pageNum, pageSize), wrapper);
        return APIRes.ok("查询成功", page);
    }

    /** GET /api/v1/order/detail/{orderId} - 订单详情（含明细） */
    @GetMapping("/detail/{orderId}")
    public APIRes detail(@PathVariable String orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return APIRes.fail(404, "订单不存在");

        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));

        // 查询支付记录
        List<PaymentRecord> payments = paymentRecordService.list(
                new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getOrderId, orderId));

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("details", details);
        result.put("payments", payments);
        return APIRes.ok("查询成功", result);
    }

    // ======================== 支付 ========================

    /**
     * PUT /api/v1/order/pay/{orderId} - 订单支付
     * 请求体：{ payAmount, payMethod(1=微信,2=支付宝,3=现金), transactionId, operatorId }
     * 支付成功后：1)更新订单状态 2)创建支付记录 3)扣减库存 4)创建销售出库单
     */
    @PutMapping("/pay/{orderId}")
    @Transactional
    public APIRes pay(@PathVariable String orderId, @RequestBody Map<String, Object> body) {
        Order order = orderService.getById(orderId);
        if (order == null) return APIRes.fail(404, "订单不存在");
        if (order.getPayStatus() != 0) return APIRes.fail(400, "订单已处理，无需重复支付");

        BigDecimal payAmount = new BigDecimal(body.get("payAmount").toString());
        Integer payMethod = Integer.valueOf(body.get("payMethod").toString());
        String transactionId = (String) body.get("transactionId");
        Integer operatorId = body.get("operatorId") != null ?
                Integer.valueOf(body.get("operatorId").toString()) : null;

        // 1. 更新订单状态：已支付→待发货
        orderService.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .set(Order::getPayStatus, 4) // 4=待发货/待取货
                .set(Order::getPayTime, new Date()));

        // 2. 创建支付记录
        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setPayAmount(payAmount);
        payment.setPayMethod(payMethod);
        payment.setTransactionId(transactionId);
        payment.setPayStatus(1);
        payment.setOperator(operatorId);
        payment.setCreateTime(new Date());
        paymentRecordService.save(payment);

        // 3. 扣减库存 + 创建销售出库单
        deductStockForOrder(orderId, operatorId);

        return APIRes.ok("支付成功", payment);
    }

    // ======================== 取消订单 ========================

    /**
     * PUT /api/v1/order/cancel/{orderId} - 取消订单（待支付状态可取消）
     */
    @PutMapping("/cancel/{orderId}")
    @Transactional
    public APIRes cancel(@PathVariable String orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return APIRes.fail(404, "订单不存在");
        if (order.getPayStatus() != 0) return APIRes.fail(400, "仅待支付订单可取消");

        orderService.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .set(Order::getPayStatus, 2)); // 2=已取消
        return APIRes.ok("订单已取消");
    }

    // ======================== 发货/完成 ========================

    /**
     * PUT /api/v1/order/ship/{orderId} - 标记发货/待取（店主/店员）
     */
    @PutMapping("/ship/{orderId}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes ship(@PathVariable String orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return APIRes.fail(404, "订单不存在");
        if (order.getPayStatus() != 4) return APIRes.fail(400, "订单状态不正确，无法发货");

        orderService.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .set(Order::getPayStatus, 1)); // 1=已发货（复用字段，实际应区分）
        return APIRes.ok("已标记发货");
    }

    /**
     * PUT /api/v1/order/complete/{orderId} - 确认收货完成（消费者/店主）
     */
    @PutMapping("/complete/{orderId}")
    public APIRes complete(@PathVariable String orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) return APIRes.fail(404, "订单不存在");

        orderService.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .set(Order::getPayStatus, 5)); // 5=已完成
        return APIRes.ok("订单已完成");
    }

    // ======================== 退款 ========================

    /**
     * POST /api/v1/order/refund/apply - 申请退款
     * 请求体：{ orderId, payId, refundAmount, refundReason, refundMethod }
     */
    @PostMapping("/refund/apply")
    @Transactional
    public APIRes refundApply(@RequestBody RefundRecord refundRecord) {
        if (refundRecord.getOrderId() == null) return APIRes.fail(400, "订单号不能为空");
        Order order = orderService.getById(refundRecord.getOrderId());
        if (order == null) return APIRes.fail(404, "订单不存在");

        refundRecord.setRefundStatus(0); // 0=待审核
        refundRecord.setCreateTime(new Date());
        refundRecordService.save(refundRecord);
        return APIRes.ok("退款申请已提交，等待审核", refundRecord);
    }

    /**
     * PUT /api/v1/order/refund/audit/{refundId} - 审核退款（店主）
     * 请求体：{ refundStatus: 1=成功, 2=拒绝 }
     */
    @PutMapping("/refund/audit/{refundId}")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public APIRes refundAudit(@PathVariable Integer refundId, @RequestBody Map<String, Object> body) {
        RefundRecord refund = refundRecordService.getById(refundId);
        if (refund == null) return APIRes.fail(404, "退款记录不存在");
        if (refund.getRefundStatus() != 0) return APIRes.fail(400, "退款申请已处理");

        Integer refundStatus = Integer.valueOf(body.get("refundStatus").toString());
        refund.setRefundStatus(refundStatus);
        refundRecordService.updateById(refund);

        if (refundStatus == 1) {
            // 退款成功：更新订单状态，恢复库存
            orderService.update(new LambdaUpdateWrapper<Order>()
                    .eq(Order::getOrderId, refund.getOrderId())
                    .set(Order::getPayStatus, 3)); // 3=已退款

            restoreStockForOrder(refund.getOrderId());
            return APIRes.ok("退款成功，库存已恢复");
        }
        return APIRes.ok("退款申请已拒绝");
    }

    /** GET /api/v1/order/refund/list - 退款列表（店主） */
    @GetMapping("/refund/list")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes refundList(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) Integer refundStatus) {
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        if (refundStatus != null) wrapper.eq(RefundRecord::getRefundStatus, refundStatus);
        wrapper.orderByDesc(RefundRecord::getCreateTime);
        Page<RefundRecord> page = refundRecordService.page(new Page<>(pageNum, pageSize), wrapper);
        return APIRes.ok("查询成功", page);
    }

    // ======================== 工具方法 ========================

    /** 支付后扣减库存，创建销售出库单 */
    private void deductStockForOrder(String orderId, Integer operatorId) {
        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));

        String outOrderId = "OUT" + new SimpleDateFormat("yyyyMMdd").format(new Date())
                + String.format("%06d", new Random().nextInt(999999));
        StockOutOrder outOrder = new StockOutOrder();
        outOrder.setOutOrderId(outOrderId);
        outOrder.setOutType(1); // 销售出库
        outOrder.setOutDate(new Date());
        outOrder.setRelatedOrderId(orderId);
        outOrder.setOperator(operatorId);
        outOrder.setCreateTime(new Date());
        outOrder.setAuditStatus(1); // 销售出库自动审核通过

        int totalQty = 0;
        List<StockOutDetail> outDetails = new ArrayList<>();
        for (OrderDetail d : details) {
            ProductSpec spec = productSpecService.getById(d.getSpecId());
            if (spec == null) continue;
            int before = spec.getStockQuantity();
            int after = Math.max(0, before - d.getSaleQuantity());
            productSpecService.update(new LambdaUpdateWrapper<ProductSpec>()
                    .eq(ProductSpec::getSpecId, d.getSpecId())
                    .set(ProductSpec::getStockQuantity, after)
                    .set(ProductSpec::getUpdateTime, new Date()));

            // 记录库存日志
            StockLog log = new StockLog();
            log.setSpecId(d.getSpecId());
            log.setChangeType(2);
            log.setChangeQuantity(-d.getSaleQuantity());
            log.setBeforeQuantity(before);
            log.setAfterQuantity(after);
            log.setRelatedOrderId(orderId);
            log.setOperator(operatorId);
            log.setCreateTime(new Date());
            stockLogService.save(log);

            StockOutDetail outDetail = new StockOutDetail();
            outDetail.setOutOrderId(outOrderId);
            outDetail.setSpecId(d.getSpecId());
            outDetail.setOutQuantity(d.getSaleQuantity());
            outDetail.setOutPrice(d.getSalePrice());
            outDetail.setSubtotal(d.getSubtotal());
            outDetails.add(outDetail);
            totalQty += d.getSaleQuantity();
        }
        outOrder.setTotalQuantity(totalQty);
        stockOutOrderService.save(outOrder);
        if (!outDetails.isEmpty()) stockOutDetailService.saveBatch(outDetails);
    }

    /** 退款后恢复库存 */
    private void restoreStockForOrder(String orderId) {
        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        for (OrderDetail d : details) {
            ProductSpec spec = productSpecService.getById(d.getSpecId());
            if (spec == null) continue;
            int before = spec.getStockQuantity();
            int after = before + d.getSaleQuantity();
            productSpecService.update(new LambdaUpdateWrapper<ProductSpec>()
                    .eq(ProductSpec::getSpecId, d.getSpecId())
                    .set(ProductSpec::getStockQuantity, after)
                    .set(ProductSpec::getUpdateTime, new Date()));

            StockLog log = new StockLog();
            log.setSpecId(d.getSpecId());
            log.setChangeType(1);
            log.setChangeQuantity(d.getSaleQuantity());
            log.setBeforeQuantity(before);
            log.setAfterQuantity(after);
            log.setRelatedOrderId(orderId);
            log.setCreateTime(new Date());
            stockLogService.save(log);
        }
    }

    private String generateOrderId() {
        return "ORDER" + new SimpleDateFormat("yyyyMMdd").format(new Date())
                + String.format("%06d", new Random().nextInt(999999));
    }
}
