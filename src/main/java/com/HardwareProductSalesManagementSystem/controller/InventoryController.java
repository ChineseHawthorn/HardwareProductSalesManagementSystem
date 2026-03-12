package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.*;
import com.HardwareProductSalesManagementSystem.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存管理控制器
 * 基础路径：/api/v1/inventory
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    @Autowired
    private ProductSpecService productSpecService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductCategoryService productCategoryService;
    @Autowired
    private StockInOrderService stockInOrderService;
    @Autowired
    private StockInDetailService stockInDetailService;
    @Autowired
    private StockOutOrderService stockOutOrderService;
    @Autowired
    private StockOutDetailService stockOutDetailService;
    @Autowired
    private StockLogService stockLogService;

    // ======================== 实时库存 ========================

    /**
     * GET /api/v1/inventory/stock/list - 实时库存列表
     * 返回各规格库存，含商品名称，标记低于预警阈值的项
     * 支持参数：pageNum, pageSize, productId, warnOnly, keyword(商品名称搜索), categoryId
     */
    @GetMapping("/stock/list")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes stockList(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "20") Integer pageSize,
                            @RequestParam(required = false) Integer productId,
                            @RequestParam(required = false) Boolean warnOnly,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer categoryId) {

        // 如果有商品名称关键词或分类筛选，先查满足条件的productId集合
        Set<Integer> productIdFilter = null;
        if (StringUtils.isNotBlank(keyword) || categoryId != null) {
            LambdaQueryWrapper<Product> pWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.isNotBlank(keyword)) {
                pWrapper.like(Product::getProductName, keyword);
            }
            if (categoryId != null) {
                List<ProductCategory> childCats = productCategoryService.list(
                        new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, categoryId));
                if (!childCats.isEmpty()) {
                    List<Integer> childIds = childCats.stream()
                            .map(ProductCategory::getCategoryId).collect(Collectors.toList());
                    pWrapper.in(Product::getCategoryId, childIds);
                } else {
                    pWrapper.eq(Product::getCategoryId, categoryId);
                }
            }
            List<Product> matchedProducts = productService.list(pWrapper);
            productIdFilter = matchedProducts.stream()
                    .map(Product::getProductId).collect(Collectors.toSet());
        }

        LambdaQueryWrapper<ProductSpec> wrapper = new LambdaQueryWrapper<>();
        if (productId != null) wrapper.eq(ProductSpec::getProductId, productId);
        if (productIdFilter != null) {
            if (productIdFilter.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("total", 0L); empty.put("pages", 0L);
                empty.put("current", 1L); empty.put("records", new ArrayList<>());
                return APIRes.ok("查询成功", empty);
            }
            wrapper.in(ProductSpec::getProductId, productIdFilter);
        }
        if (Boolean.TRUE.equals(warnOnly)) {
            wrapper.apply("stock_quantity <= warning_threshold");
        }

        Page<ProductSpec> page = productSpecService.page(new Page<>(pageNum, pageSize), wrapper);

        // 批量加载商品信息
        List<ProductSpec> specs = page.getRecords();
        Set<Integer> pIds = specs.stream().map(ProductSpec::getProductId).collect(Collectors.toSet());
        Map<Integer, Product> productMap = new HashMap<>();
        if (!pIds.isEmpty()) {
            productService.listByIds(pIds).forEach(p -> productMap.put(p.getProductId(), p));
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (ProductSpec spec : specs) {
            Map<String, Object> item = specToMap(spec);
            Product p = productMap.get(spec.getProductId());
            item.put("productName", p != null ? p.getProductName() : null);
            item.put("categoryId", p != null ? p.getCategoryId() : null);
            item.put("isOnSale", p != null ? p.getIsOnSale() : null);
            item.put("isWarning", spec.getStockQuantity() != null
                    && spec.getWarningThreshold() != null
                    && spec.getStockQuantity() <= spec.getWarningThreshold());
            records.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", records);
        return APIRes.ok("查询成功", result);
    }

    // ======================== 入库管理 ========================

    /**
     * POST /api/v1/inventory/in/add - 创建入库单（店主直接生效，店员提交待审核）
     * 请求体：{ supplierId, remark, details:[{specId, inQuantity, purchasePrice}] }
     */
    @PostMapping("/in/add")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @Transactional
    public APIRes stockInAdd(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) body.get("details");
        if (details == null || details.isEmpty()) {
            return APIRes.fail(400, "入库明细不能为空");
        }

        // 获取当前操作人ID（从SecurityContext中获取，此处简化）
        String inOrderId = generateOrderId("IN");
        StockInOrder order = new StockInOrder();
        order.setInOrderId(inOrderId);
        order.setInDate(new Date());
        order.setCreateTime(new Date());
        order.setRemark((String) body.get("remark"));
        order.setOperator((Integer) body.get("operatorId"));
        order.setSupplierId(body.get("supplierId") != null ?
                Integer.valueOf(body.get("supplierId").toString()) : null);
        // 店员提交待审核，店主直接通过
        boolean isOwner = isCurrentUserOwner();
        order.setAuditStatus(isOwner ? 1 : 0);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<StockInDetail> detailList = new ArrayList<>();

        for (Map<String, Object> d : details) {
            Integer specId = Integer.valueOf(d.get("specId").toString());
            Integer qty = Integer.valueOf(d.get("inQuantity").toString());
            BigDecimal price = new BigDecimal(d.get("purchasePrice").toString());
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);

            StockInDetail detail = new StockInDetail();
            detail.setInOrderId(inOrderId);
            detail.setSpecId(specId);
            detail.setInQuantity(qty);
            detail.setPurchasePrice(price);
            detail.setSubtotal(subtotal);
            detailList.add(detail);
        }
        order.setTotalAmount(totalAmount);
        stockInOrderService.save(order);
        stockInDetailService.saveBatch(detailList);

        // 店主操作立即更新库存
        if (isOwner) {
            updateStockAfterIn(detailList, inOrderId, order.getOperator());
        }

        return APIRes.ok(isOwner ? "入库成功" : "入库申请已提交，等待审核", order);
    }

    /**
     * PUT /api/v1/inventory/in/audit/{id} - 审核入库单（店主）
     * 请求体：{ auditStatus: 1=通过, 2=拒绝 }
     */
    @PutMapping("/in/audit/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public APIRes stockInAudit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        StockInOrder order = stockInOrderService.getById(id);
        if (order == null) return APIRes.fail(404, "入库单不存在");
        if (order.getAuditStatus() != 0) return APIRes.fail(400, "该入库单已审核");

        Integer auditStatus = Integer.valueOf(body.get("auditStatus").toString());
        order.setAuditStatus(auditStatus);
        stockInOrderService.updateById(order);

        if (auditStatus == 1) {
            // 通过：更新库存
            List<StockInDetail> details = stockInDetailService.list(
                    new LambdaQueryWrapper<StockInDetail>().eq(StockInDetail::getInOrderId, id));
            updateStockAfterIn(details, id, order.getOperator());
            return APIRes.ok("审核通过，库存已更新");
        }
        return APIRes.ok("已拒绝该入库申请");
    }

    /** GET /api/v1/inventory/in/list - 入库单列表（店主/店员） */
    @GetMapping("/in/list")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes stockInList(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(required = false) Integer auditStatus) {
        LambdaQueryWrapper<StockInOrder> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) wrapper.eq(StockInOrder::getAuditStatus, auditStatus);
        wrapper.orderByDesc(StockInOrder::getCreateTime);
        Page<StockInOrder> page = stockInOrderService.page(new Page<>(pageNum, pageSize), wrapper);
        return APIRes.ok("查询成功", page);
    }

    // ======================== 出库管理 ========================

    /**
     * POST /api/v1/inventory/out/add - 创建出库单（损耗/调拨，店主/店员）
     * 请求体：{ outType, remark, details:[{specId, outQuantity, outPrice}] }
     * 注意：销售出库由订单支付时自动触发，不走此接口
     */
    @PostMapping("/out/add")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @Transactional
    public APIRes stockOutAdd(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) body.get("details");
        if (details == null || details.isEmpty()) {
            return APIRes.fail(400, "出库明细不能为空");
        }
        Integer outType = Integer.valueOf(body.get("outType").toString());
        if (outType == 1) return APIRes.fail(400, "销售出库请通过订单流程处理");

        String outOrderId = generateOrderId("OUT");
        StockOutOrder order = new StockOutOrder();
        order.setOutOrderId(outOrderId);
        order.setOutType(outType);
        order.setOutDate(new Date());
        order.setCreateTime(new Date());
        order.setRemark((String) body.get("remark"));
        order.setOperator((Integer) body.get("operatorId"));
        boolean isOwner = isCurrentUserOwner();
        order.setAuditStatus(isOwner ? 1 : 0);

        int totalQty = 0;
        List<StockOutDetail> detailList = new ArrayList<>();
        for (Map<String, Object> d : details) {
            Integer specId = Integer.valueOf(d.get("specId").toString());
            Integer qty = Integer.valueOf(d.get("outQuantity").toString());
            BigDecimal price = d.get("outPrice") != null ?
                    new BigDecimal(d.get("outPrice").toString()) : BigDecimal.ZERO;
            totalQty += qty;

            StockOutDetail detail = new StockOutDetail();
            detail.setOutOrderId(outOrderId);
            detail.setSpecId(specId);
            detail.setOutQuantity(qty);
            detail.setOutPrice(price);
            detail.setSubtotal(price.multiply(BigDecimal.valueOf(qty)));
            detailList.add(detail);
        }
        order.setTotalQuantity(totalQty);
        stockOutOrderService.save(order);
        stockOutDetailService.saveBatch(detailList);

        if (isOwner) {
            updateStockAfterOut(detailList, outOrderId, order.getOperator());
        }
        return APIRes.ok(isOwner ? "出库成功" : "出库申请已提交，等待审核", order);
    }

    /**
     * PUT /api/v1/inventory/out/audit/{id} - 审核出库单（店主）
     */
    @PutMapping("/out/audit/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public APIRes stockOutAudit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        StockOutOrder order = stockOutOrderService.getById(id);
        if (order == null) return APIRes.fail(404, "出库单不存在");
        if (order.getAuditStatus() != 0) return APIRes.fail(400, "该出库单已审核");

        Integer auditStatus = Integer.valueOf(body.get("auditStatus").toString());
        order.setAuditStatus(auditStatus);
        stockOutOrderService.updateById(order);

        if (auditStatus == 1) {
            List<StockOutDetail> details = stockOutDetailService.list(
                    new LambdaQueryWrapper<StockOutDetail>().eq(StockOutDetail::getOutOrderId, id));
            updateStockAfterOut(details, id, order.getOperator());
            return APIRes.ok("审核通过，库存已扣减");
        }
        return APIRes.ok("已拒绝该出库申请");
    }

    // ======================== 库存日志 ========================

    /**
     * GET /api/v1/inventory/log/list - 库存变动日志
     */
    @GetMapping("/log/list")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes logList(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "20") Integer pageSize,
                          @RequestParam(required = false) Integer specId,
                          @RequestParam(required = false) Integer changeType) {
        LambdaQueryWrapper<StockLog> wrapper = new LambdaQueryWrapper<>();
        if (specId != null) wrapper.eq(StockLog::getSpecId, specId);
        if (changeType != null) wrapper.eq(StockLog::getChangeType, changeType);
        wrapper.orderByDesc(StockLog::getCreateTime);
        Page<StockLog> page = stockLogService.page(new Page<>(pageNum, pageSize), wrapper);
        return APIRes.ok("查询成功", page);
    }

    // ======================== 工具方法 ========================

    /** 入库后更新ProductSpec库存并记录日志 */
    private void updateStockAfterIn(List<StockInDetail> details, String orderId, Integer operator) {
        for (StockInDetail d : details) {
            ProductSpec spec = productSpecService.getById(d.getSpecId());
            if (spec == null) continue;
            int before = spec.getStockQuantity();
            int after = before + d.getInQuantity();
            productSpecService.update(new LambdaUpdateWrapper<ProductSpec>()
                    .eq(ProductSpec::getSpecId, d.getSpecId())
                    .set(ProductSpec::getStockQuantity, after)
                    .set(ProductSpec::getUpdateTime, new Date()));
            saveStockLog(d.getSpecId(), 1, d.getInQuantity(), before, after, orderId, operator);
        }
    }

    /** 出库后更新ProductSpec库存并记录日志 */
    private void updateStockAfterOut(List<StockOutDetail> details, String orderId, Integer operator) {
        for (StockOutDetail d : details) {
            ProductSpec spec = productSpecService.getById(d.getSpecId());
            if (spec == null) continue;
            int before = spec.getStockQuantity();
            int after = Math.max(0, before - d.getOutQuantity());
            productSpecService.update(new LambdaUpdateWrapper<ProductSpec>()
                    .eq(ProductSpec::getSpecId, d.getSpecId())
                    .set(ProductSpec::getStockQuantity, after)
                    .set(ProductSpec::getUpdateTime, new Date()));
            saveStockLog(d.getSpecId(), 2, -d.getOutQuantity(), before, after, orderId, operator);
        }
    }

    private void saveStockLog(Integer specId, Integer changeType, Integer changeQty,
                              Integer before, Integer after, String relatedOrderId, Integer operator) {
        StockLog log = new StockLog();
        log.setSpecId(specId);
        log.setChangeType(changeType);
        log.setChangeQuantity(changeQty);
        log.setBeforeQuantity(before);
        log.setAfterQuantity(after);
        log.setRelatedOrderId(relatedOrderId);
        log.setOperator(operator);
        log.setCreateTime(new Date());
        stockLogService.save(log);
    }

    private String generateOrderId(String prefix) {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int rand = new Random().nextInt(999999);
        return prefix + date + String.format("%06d", rand);
    }

    private boolean isCurrentUserOwner() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority()));
    }

    private Map<String, Object> specToMap(ProductSpec spec) {
        Map<String, Object> map = new HashMap<>();
        map.put("specId", spec.getSpecId());
        map.put("productId", spec.getProductId());
        map.put("material", spec.getMaterial());
        map.put("size", spec.getSize());
        map.put("unitPrice", spec.getUnitPrice());
        map.put("unit", spec.getUnit());
        map.put("stockQuantity", spec.getStockQuantity());
        map.put("warningThreshold", spec.getWarningThreshold());
        map.put("specCode", spec.getSpecCode());
        return map;
    }
}
