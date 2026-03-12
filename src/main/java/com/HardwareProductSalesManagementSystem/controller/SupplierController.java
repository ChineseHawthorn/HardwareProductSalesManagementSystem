package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.Supplier;
import com.HardwareProductSalesManagementSystem.service.SupplierService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 供应商管理控制器
 * 基础路径：/api/v1/supplier
 */
@RestController
@RequestMapping("/api/v1/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    /** GET /api/v1/supplier/list - 分页查询供应商列表 */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(Supplier::getSupplierName, keyword)
                   .or().like(Supplier::getContactPerson, keyword)
                   .or().like(Supplier::getContactPhone, keyword);
        }
        if (status != null) wrapper.eq(Supplier::getStatus, status);
        wrapper.orderByDesc(Supplier::getCreateTime);
        Page<Supplier> page = supplierService.page(new Page<>(pageNum, pageSize), wrapper);
        return APIRes.ok("查询成功", page);
    }

    /** GET /api/v1/supplier/{id} - 查询供应商详情 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes detail(@PathVariable Integer id) {
        Supplier supplier = supplierService.getById(id);
        if (supplier == null) return APIRes.fail(404, "供应商不存在");
        return APIRes.ok("查询成功", supplier);
    }

    /** POST /api/v1/supplier/add - 新增供应商（店主） */
    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes add(@RequestBody Supplier supplier) {
        if (StringUtils.isBlank(supplier.getSupplierName())) {
            return APIRes.fail(400, "供应商名称不能为空");
        }
        if (StringUtils.isBlank(supplier.getContactPhone())) {
            return APIRes.fail(400, "联系电话不能为空");
        }
        // 检查名称唯一性
        Long nameCount = supplierService.count(
                new LambdaQueryWrapper<Supplier>().eq(Supplier::getSupplierName, supplier.getSupplierName()));
        if (nameCount > 0) return APIRes.fail(400, "供应商名称已存在");

        supplier.setCreateTime(new Date());
        supplier.setUpdateTime(new Date());
        if (supplier.getStatus() == null) supplier.setStatus(1);
        supplierService.save(supplier);
        return APIRes.ok("新增供应商成功", supplier);
    }

    /** PUT /api/v1/supplier/update/{id} - 修改供应商（店主） */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes update(@PathVariable Integer id, @RequestBody Supplier supplier) {
        supplier.setSupplierId(id);
        supplier.setUpdateTime(new Date());
        supplierService.updateById(supplier);
        return APIRes.ok("修改成功");
    }

    /** PUT /api/v1/supplier/status/{id} - 启用/禁用供应商（店主） */
    @PutMapping("/status/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public APIRes status(@PathVariable Integer id, @RequestParam Integer status) {
        Supplier s = new Supplier();
        s.setSupplierId(id);
        s.setStatus(status);
        s.setUpdateTime(new Date());
        supplierService.updateById(s);
        return APIRes.ok(status == 1 ? "已启用" : "已禁用");
    }
}
