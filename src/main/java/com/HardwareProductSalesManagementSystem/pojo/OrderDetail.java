package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细表
 */
@TableName("order_detail")
@Data
public class OrderDetail {
    @TableId(type = IdType.AUTO)
    private Integer detailId;

    /** 关联订单号 */
    private String orderId;

    /** 关联规格ID */
    private Integer specId;

    /** 销售数量 */
    private Integer saleQuantity;

    /** 实际售价（经折扣计算后） */
    private BigDecimal salePrice;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 是否已退款：0=否，1=是 */
    private Integer isRefunded;
}
