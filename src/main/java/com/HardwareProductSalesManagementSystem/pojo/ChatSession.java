package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 客服会话表
 * status: 0=待接入, 1=进行中, 2=已关闭
 */
@TableName("chat_session")
@Data
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Integer sessionId;

    /** 消费者用户ID */
    private Integer consumerId;

    /** 接待店员ID（null=未分配） */
    private Integer staffId;

    /** 会话标题 */
    private String title;

    /** 状态：0=待接入，1=进行中，2=已关闭 */
    private Integer status;

    private Date createTime;
    private Date updateTime;
}
