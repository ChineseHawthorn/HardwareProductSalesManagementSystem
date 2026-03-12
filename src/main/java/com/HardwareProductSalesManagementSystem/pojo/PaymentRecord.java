package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付记录表
 */
@TableName("payment_record")
@Data
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Integer payId;

    /** 关联订单号 */
    private String orderId;

    /** 实际支付金额 */
    private BigDecimal payAmount;

    /** 支付方式：1=微信，2=支付宝，3=现金 */
    private Integer payMethod;

    /** 第三方交易流水号 */
    private String transactionId;

    /** 支付状态：1=成功，2=失败 */
    private Integer payStatus;

    /** 操作人user_id（线下收款时为店员ID） */
    private Integer operator;

    private Date createTime;
}
