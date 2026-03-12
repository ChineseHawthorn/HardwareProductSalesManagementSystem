package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单表（注意：order是MySQL保留字，需用反引号）
 * 订单号格式：ORDER+YYYYMMDD+6位随机数字
 */
@TableName("`order`")
@Data
public class Order {
    /** 订单号（字符串主键，手动生成） */
    @TableId(type = IdType.INPUT)
    private String orderId;

    /** 下单用户ID */
    private Integer userId;

    /** 订单总金额 */
    private BigDecimal orderAmount;

    /**
     * 支付状态：0=待支付，1=已支付，2=已取消，3=已退款，
     * 4=待发货/待取货，5=已完成
     */
    private Integer payStatus;

    /** 订单类型：1=线上，2=线下 */
    private Integer orderType;

    /** 交易类型：1=批发，2=零售 */
    private Integer tradeType;

    private String receiverName;
    private String receiverPhone;

    /** 收货地址（线下订单为NULL） */
    private String receiverAddr;

    private Date createTime;
    private Date payTime;
    private String remark;
}
