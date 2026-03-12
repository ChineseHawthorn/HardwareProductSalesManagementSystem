package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 购物车表
 */
@TableName("cart")
@Data
public class Cart {
    @TableId(type = IdType.AUTO)
    private Integer cartId;

    /** 用户ID */
    private Integer userId;

    /** 商品规格ID */
    private Integer specId;

    /** 购买数量 */
    private Integer quantity;

    private Date createTime;
    private Date updateTime;
}
