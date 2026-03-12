package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品分类表
 */
@TableName("product_category")
@Data
public class ProductCategory {
    @TableId(type = IdType.AUTO)
    private Integer categoryId;

    /** 父分类ID（0表示顶级分类） */
    private Integer parentId;

    /** 分类名称 */
    private String categoryName;

    /** 状态：0=禁用，1=正常 */
    private Integer status;

    /** 排序序号 */
    private Integer sortOrder;

    private Date createTime;
    private Date updateTime;
}
