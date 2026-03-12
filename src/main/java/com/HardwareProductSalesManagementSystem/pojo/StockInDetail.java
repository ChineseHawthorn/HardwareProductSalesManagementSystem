package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库单明细表
 */
@TableName("stock_in_detail")
@Data
public class StockInDetail {
    @TableId(type = IdType.AUTO)
    private Integer detailId;

    /** 关联入库单号 */
    private String inOrderId;

    /** 关联规格ID */
    private Integer specId;

    /** 入库数量（≥1） */
    private Integer inQuantity;

    /** 进货单价 */
    private BigDecimal purchasePrice;

    /** 小计金额 */
    private BigDecimal subtotal;
}
