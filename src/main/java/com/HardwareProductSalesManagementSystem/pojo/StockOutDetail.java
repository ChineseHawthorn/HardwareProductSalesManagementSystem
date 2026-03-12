package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出库单明细表
 */
@TableName("stock_out_detail")
@Data
public class StockOutDetail {
    @TableId(type = IdType.AUTO)
    private Integer detailId;

    /** 关联出库单号 */
    private String outOrderId;

    /** 关联规格ID */
    private Integer specId;

    /** 出库数量 */
    private Integer outQuantity;

    /** 出库单价 */
    private BigDecimal outPrice;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 批次号 */
    private String batchNo;
}
