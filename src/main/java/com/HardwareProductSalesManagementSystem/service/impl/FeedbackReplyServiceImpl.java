package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.FeedbackReplyMapper;
import com.HardwareProductSalesManagementSystem.pojo.FeedbackReply;
import com.HardwareProductSalesManagementSystem.service.FeedbackReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FeedbackReplyServiceImpl extends ServiceImpl<FeedbackReplyMapper, FeedbackReply>
        implements FeedbackReplyService {
}
