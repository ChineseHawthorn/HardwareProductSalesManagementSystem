package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 角色表（对齐数据表role）
 * @TableName role
 */
@TableName(value = "role")
@Data
public class Role {
    @TableId(type = IdType.AUTO)
    private Integer roleId;

    /** 角色名称（非空：店主/店员/消费者） */
    private String roleName;

    /** 角色类型：0=个人消费者，1=企业客户，2=店员，3=店主 */
    private Integer roleType;

    /** 角色描述（允许NULL） */
    private String roleDesc;

    private Date createTime;
}