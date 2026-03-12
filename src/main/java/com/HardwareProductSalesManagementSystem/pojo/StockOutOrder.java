package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 出库单表（主表）
 * 单号格式：OUT+YYYYMMDD+6位随机数字
 */
@TableName("stock_out_order")
@Data
public class StockOutOrder {
    /** 出库单号（字符串主键，手动生成） */
    @TableId(type = IdType.INPUT)
    private String outOrderId;

    /** 出库类型：1=销售出库，2=损耗出库，3=调拨出库 */
    private Integer outType;

    private Date outDate;

    private Integer totalQuantity;

    /** 关联订单ID（销售出库时关联order） */
    private String relatedOrderId;

    /** 凭证URL */
    private String certificateUrl;

    /** 操作人user_id */
    private Integer operator;

    private Date createTime;

    private String remark;

    /** 审核状态：0=待审核，1=已通过，2=已拒绝 */
    private Integer auditStatus;
}
