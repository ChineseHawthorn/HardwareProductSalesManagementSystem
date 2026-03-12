package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 库存变动日志表
 */
@TableName("stock_log")
@Data
public class StockLog {
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 关联规格ID */
    private Integer specId;

    /** 变动类型：1=入库，2=出库 */
    private Integer changeType;

    /** 变动数量（正数=入库，负数=出库） */
    private Integer changeQuantity;

    /** 变动前库存 */
    private Integer beforeQuantity;

    /** 变动后库存 */
    private Integer afterQuantity;

    /** 关联单据ID（入库单/出库单/订单） */
    private String relatedOrderId;

    /** 操作人user_id */
    private Integer operator;

    private Date createTime;
}
