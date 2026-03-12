package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 角色权限表
 */
@TableName("role_permission")
@Data
public class RolePermission {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联角色ID */
    private Integer roleId;

    /** 权限码（格式：模块:操作，如 product:add） */
    private String permissionCode;

    /** 权限名称（中文描述） */
    private String permissionName;

    private Date createTime;
}
