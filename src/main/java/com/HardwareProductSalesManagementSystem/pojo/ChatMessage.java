package com.HardwareProductSalesManagementSystem.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 聊天消息表
 * senderRole: 0=消费者, 1=店员/店主
 * msgType:    0=文字,   1=图片
 * isRead:     0=未读,   1=已读
 */
@TableName("chat_message")
@Data
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long msgId;

    /** 所属会话ID */
    private Integer sessionId;

    /** 发送者用户ID */
    private Integer senderId;

    /** 发送者角色：0=消费者，1=店员/店主 */
    private Integer senderRole;

    /** 消息内容 */
    private String content;

    /** 消息类型：0=文字，1=图片 */
    private Integer msgType;

    /** 是否已读：0=未读，1=已读 */
    private Integer isRead;

    private Date createTime;
}
