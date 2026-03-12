package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Integer feedbackId;

    /** 提交人user_id，可为null（匿名） */
    private Integer userId;

    /** 反馈标题 */
    private String title;

    /** 反馈内容 */
    private String content;

    /** 状态：0=未回复，1=已回复，2=已关闭 */
    private Integer status;

    /** 关联商品ID（可选） */
    private Integer productId;

    /** 关联订单ID（可选） */
    private String orderId;

    private Date createTime;
    private Date updateTime;
}
