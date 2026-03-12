package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品表
 */
@TableName("product")
@Data
public class Product {
    @TableId(type = IdType.AUTO)
    private Integer productId;

    private String productName;
    private String brand;
    private String model;

    /** 关联product_category */
    private Integer categoryId;

    private String description;

    /** 是否上架：0=下架，1=上架 */
    private Integer isOnSale;

    /** 创建人（user_id） */
    private Integer createUser;

    private Date createTime;
    private Date updateTime;
}
