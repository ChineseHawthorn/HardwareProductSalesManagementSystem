package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品图片表
 */
@TableName("product_image")
@Data
public class ProductImage {
    @TableId(type = IdType.AUTO)
    private Integer imageId;

    /** 关联product */
    private Integer productId;

    private String imageUrl;

    /** 图片排序：1=主图，2-5=副图 */
    private Integer imageSort;

    private Date uploadTime;

    /** 是否删除：0=正常，1=已删除 */
    private Integer isDeleted;
}
