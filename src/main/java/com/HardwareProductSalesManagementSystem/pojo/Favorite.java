package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品收藏表
 */
@TableName("favorite")
@Data
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Integer favoriteId;
    private Integer userId;
    private Integer productId;
    private Date createTime;
}
