package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 价格策略表
 */
@TableName("price_strategy")
@Data
public class PriceStrategy {
    @TableId(type = IdType.AUTO)
    private Integer strategyId;

    private String strategyName;

    /** 策略类型：1=按规格，2=按分类，3=全局 */
    private Integer strategyType;

    /** 关联商品ID（策略类型=1时有效） */
    private Integer productId;

    /** 关联分类ID（策略类型=2时有效） */
    private Integer categoryId;

    /** 最小购买数量（满足该数量才享受折扣） */
    private Integer minQuantity;

    /** 折扣类型：1=折扣比例，2=固定减额，3=固定价格 */
    private Integer discountType;

    /** 折扣值（折扣比例时为0-100的百分比） */
    private BigDecimal discountValue;

    private Date startTime;

    /** 结束时间（NULL=永久有效） */
    private Date endTime;

    /** 适用角色：0=个人，1=企业，2=全部 */
    private Integer applicableRole;

    /** 状态：0=禁用，1=启用 */
    private Integer status;

    /** 创建人user_id */
    private Integer creator;

    private Date createTime;
    private Date updateTime;
}
