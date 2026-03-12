package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 供应商表
 */
@TableName("supplier")
@Data
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Integer supplierId;

    /** 供应商名称（唯一） */
    private String supplierName;

    private String contactPerson;

    /** 联系电话（11位，唯一） */
    private String contactPhone;

    private String supplierAddr;

    /** 资质证书URL */
    private String qualificationUrl;

    /** 状态：0=禁用，1=正常 */
    private Integer status;

    private Date createTime;
    private Date updateTime;

    /** 操作人user_id */
    private Integer operator;
}
