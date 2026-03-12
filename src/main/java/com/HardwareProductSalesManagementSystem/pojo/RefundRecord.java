package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款记录表
 */
@TableName("refund_record")
@Data
public class RefundRecord {
    @TableId(type = IdType.AUTO)
    private Integer refundId;

    /** 关联订单号 */
    private String orderId;

    /** 关联支付记录 */
    private Integer payId;

    /** 退款金额（≤实际支付金额） */
    private BigDecimal refundAmount;

    private String refundReason;

    /** 退款方式：1=原路退回，2=现金退款 */
    private Integer refundMethod;

    /** 退款状态：0=待审核，1=退款成功，2=退款失败 */
    private Integer refundStatus;

    /** 操作人user_id */
    private Integer operator;

    private Date createTime;
}
