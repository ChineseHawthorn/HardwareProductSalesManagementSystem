package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 入库单表（主表）
 * 单号格式：IN+YYYYMMDD+6位随机数字
 */
@TableName("stock_in_order")
@Data
public class StockInOrder {
    /** 入库单号（字符串主键，手动生成） */
    @TableId(type = IdType.INPUT)
    private String inOrderId;

    /** 入库日期 */
    private Date inDate;

    /** 入库总金额 */
    private BigDecimal totalAmount;

    /** 凭证图片URL */
    private String certificateUrl;

    /** 操作人user_id */
    private Integer operator;

    private Date createTime;

    private String remark;

    /** 审核状态：0=待审核，1=已通过，2=已拒绝 */
    private Integer auditStatus;

    /** 关联供应商ID */
    private Integer supplierId;
}
