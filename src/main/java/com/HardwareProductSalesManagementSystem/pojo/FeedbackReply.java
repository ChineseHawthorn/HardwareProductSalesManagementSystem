package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("feedback_reply")
public class FeedbackReply {

    @TableId(type = IdType.AUTO)
    private Integer replyId;

    /** 关联反馈ID */
    private Integer feedbackId;

    /** 回复人user_id */
    private Integer replyUserId;

    /** 回复内容 */
    private String content;

    private Date createTime;
}
