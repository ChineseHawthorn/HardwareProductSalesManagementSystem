package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 用户表（严格对齐数据表user）
 * @TableName user
 */
@TableName(value = "user")
@Data
public class User {
    /**
     * 主键，自增；唯一标识用户
     */
    @TableId(type = IdType.AUTO)
    private Integer userId;

    /**
     * 账号（非空，唯一）
     */
    private String username;

    /**
     * 密码（MD5加盐加密，非空）
     */
    private String password;

    /**
     * 头像地址（默认./images/avatar/default.png）
     */
    private String avatar;

    /**
     * 角色ID（非空，关联role表role_id）
     */
    private Integer roleId;

    /**
     * 真实姓名（允许NULL）
     */
    private String realName;

    /**
     * 手机号（11位，唯一，非空）
     */
    private String phone;

    /**
     * 账号状态：0=禁用，1=正常（默认1）
     */
    private Integer status;

    /**
     * 创建时间（非空）
     */
    private Date createTime;

    /**
     * 最后登录时间（首次登录前为NULL）
     */
    private Date lastLoginTime;

    // 创建时默认账号状态为正常
    @TableField(exist = false)
    public static final Integer DEFAULT_STATUS = 1;

    // 头像默认地址
    @TableField(exist = false)
    public static final String DEFAULT_AVATAR = "./images/avatar/default.png";

    // role_id默认值为2
    @TableField(exist = false)
    public static final Integer DEFAULT_ROLE_ID = 2;
}