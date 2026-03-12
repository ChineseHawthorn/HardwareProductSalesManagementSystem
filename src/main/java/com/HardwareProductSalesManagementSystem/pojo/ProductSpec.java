package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品规格表
 */
@TableName("product_spec")
@Data
public class ProductSpec {
    @TableId(type = IdType.AUTO)
    private Integer specId;

    /** 关联product */
    private Integer productId;

    /** 材质 */
    private String material;

    /** 尺寸/规格 */
    private String size;

    /** 零售单价 */
    private BigDecimal unitPrice;

    /** 单位（个/套/包/kg） */
    private String unit;

    /** 当前库存数量 */
    private Integer stockQuantity;

    /** 库存预警阈值（默认10） */
    private Integer warningThreshold;

    /** 规格编码（唯一，如 WG-TJ-001） */
    private String specCode;

    private Date updateTime;
}
