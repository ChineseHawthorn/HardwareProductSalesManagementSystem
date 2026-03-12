package com.HardwareProductSalesManagementSystem.service.impl;

import com.HardwareProductSalesManagementSystem.mapper.RefundRecordMapper;
import com.HardwareProductSalesManagementSystem.pojo.RefundRecord;
import com.HardwareProductSalesManagementSystem.service.RefundRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord>
        implements RefundRecordService {
}
