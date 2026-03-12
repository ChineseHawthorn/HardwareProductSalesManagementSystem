package com.HardwareProductSalesManagementSystem.controller;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.pojo.Feedback;
import com.HardwareProductSalesManagementSystem.pojo.FeedbackReply;
import com.HardwareProductSalesManagementSystem.pojo.User;
import com.HardwareProductSalesManagementSystem.service.FeedbackReplyService;
import com.HardwareProductSalesManagementSystem.service.FeedbackService;
import com.HardwareProductSalesManagementSystem.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 反馈管理控制器
 * 基础路径：/api/v1/feedback
 */
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private FeedbackReplyService feedbackReplyService;
    @Autowired
    private UserService userService;

    /**
     * POST /api/v1/feedback/add - 提交反馈（消费者/已登录用户）
     * 请求体：{ userId, title, content, productId(可选), orderId(可选) }
     */
    @PostMapping("/add")
    public APIRes add(@RequestBody Feedback feedback) {
        if (StringUtils.isBlank(feedback.getTitle())) return APIRes.fail(400, "反馈标题不能为空");
        if (StringUtils.isBlank(feedback.getContent())) return APIRes.fail(400, "反馈内容不能为空");
        feedback.setStatus(0);
        feedback.setCreateTime(new Date());
        feedback.setUpdateTime(new Date());
        feedbackService.save(feedback);
        return APIRes.ok("反馈提交成功", feedback);
    }

    /**
     * GET /api/v1/feedback/list - 反馈列表（店主/店员）
     * 支持筛选：status, keyword
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "15") Integer pageSize,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Feedback::getStatus, status);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Feedback::getTitle, keyword)
                    .or().like(Feedback::getContent, keyword));
        }
        wrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = feedbackService.page(new Page<>(pageNum, pageSize), wrapper);

        // 附加提交人用户名
        List<Map<String, Object>> records = new ArrayList<>();
        for (Feedback fb : page.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("feedbackId", fb.getFeedbackId());
            row.put("userId", fb.getUserId());
            row.put("title", fb.getTitle());
            row.put("content", fb.getContent());
            row.put("status", fb.getStatus());
            row.put("productId", fb.getProductId());
            row.put("orderId", fb.getOrderId());
            row.put("createTime", fb.getCreateTime());
            row.put("updateTime", fb.getUpdateTime());

            if (fb.getUserId() != null) {
                User u = userService.getById(fb.getUserId());
                row.put("username", u != null ? u.getUsername() : "未知用户");
                row.put("realName", u != null ? u.getRealName() : null);
                row.put("phone", u != null ? u.getPhone() : null);
            } else {
                row.put("username", "匿名用户");
                row.put("realName", null);
                row.put("phone", null);
            }
            records.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", records);
        return APIRes.ok("查询成功", result);
    }

    /**
     * GET /api/v1/feedback/detail/{id} - 反馈详情（含所有回复及用户信息）
     */
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes detail(@PathVariable Integer id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) return APIRes.fail(404, "反馈不存在");

        // 提交人信息
        Map<String, Object> fbMap = new HashMap<>();
        fbMap.put("feedbackId", feedback.getFeedbackId());
        fbMap.put("userId", feedback.getUserId());
        fbMap.put("title", feedback.getTitle());
        fbMap.put("content", feedback.getContent());
        fbMap.put("status", feedback.getStatus());
        fbMap.put("productId", feedback.getProductId());
        fbMap.put("orderId", feedback.getOrderId());
        fbMap.put("createTime", feedback.getCreateTime());

        if (feedback.getUserId() != null) {
            User u = userService.getById(feedback.getUserId());
            fbMap.put("username", u != null ? u.getUsername() : "未知用户");
            fbMap.put("realName", u != null ? u.getRealName() : null);
        } else {
            fbMap.put("username", "匿名用户");
            fbMap.put("realName", null);
        }

        // 所有回复
        List<FeedbackReply> replies = feedbackReplyService.list(
                new LambdaQueryWrapper<FeedbackReply>()
                        .eq(FeedbackReply::getFeedbackId, id)
                        .orderByAsc(FeedbackReply::getCreateTime));

        List<Map<String, Object>> replyList = new ArrayList<>();
        for (FeedbackReply r : replies) {
            Map<String, Object> rMap = new HashMap<>();
            rMap.put("replyId", r.getReplyId());
            rMap.put("feedbackId", r.getFeedbackId());
            rMap.put("replyUserId", r.getReplyUserId());
            rMap.put("content", r.getContent());
            rMap.put("createTime", r.getCreateTime());
            if (r.getReplyUserId() != null) {
                User u = userService.getById(r.getReplyUserId());
                rMap.put("replyUsername", u != null ? u.getUsername() : "客服");
            } else {
                rMap.put("replyUsername", "客服");
            }
            replyList.add(rMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("feedback", fbMap);
        result.put("replies", replyList);
        return APIRes.ok("查询成功", result);
    }

    /**
     * POST /api/v1/feedback/reply - 回复反馈（店主/店员）
     * 请求体：{ feedbackId, replyUserId, content }
     */
    @PostMapping("/reply")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes reply(@RequestBody FeedbackReply reply) {
        if (reply.getFeedbackId() == null) return APIRes.fail(400, "反馈ID不能为空");
        if (StringUtils.isBlank(reply.getContent())) return APIRes.fail(400, "回复内容不能为空");

        reply.setCreateTime(new Date());
        feedbackReplyService.save(reply);

        // 更新反馈状态为已回复
        Feedback fb = new Feedback();
        fb.setFeedbackId(reply.getFeedbackId());
        fb.setStatus(1);
        fb.setUpdateTime(new Date());
        feedbackService.updateById(fb);

        return APIRes.ok("回复成功", reply);
    }

    /**
     * PUT /api/v1/feedback/status/{id} - 更新反馈状态（店主/店员）
     * 请求参数：status (0=未回复, 1=已回复, 2=已关闭)
     */
    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    public APIRes updateStatus(@PathVariable Integer id, @RequestParam Integer status) {
        Feedback fb = feedbackService.getById(id);
        if (fb == null) return APIRes.fail(404, "反馈不存在");
        fb.setStatus(status);
        fb.setUpdateTime(new Date());
        feedbackService.updateById(fb);
        String msg = status == 2 ? "已关闭该反馈" : (status == 1 ? "已标记为已回复" : "已标记为未回复");
        return APIRes.ok(msg);
    }

    /**
     * GET /api/v1/feedback/order/{orderId} - 消费者获取指定订单的反馈详情（含所有回复）
     * 请求参数：userId - 当前用户ID（只能查询自己的反馈）
     */
    @GetMapping("/order/{orderId}")
    public APIRes getByOrder(@PathVariable String orderId, @RequestParam Integer userId) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getOrderId, orderId)
               .eq(Feedback::getUserId, userId)
               .orderByDesc(Feedback::getCreateTime)
               .last("LIMIT 1");

        Feedback feedback = feedbackService.getOne(wrapper);
        if (feedback == null) {
            return APIRes.ok("暂无反馈", null);
        }

        Map<String, Object> fbMap = new HashMap<>();
        fbMap.put("feedbackId", feedback.getFeedbackId());
        fbMap.put("userId", feedback.getUserId());
        fbMap.put("title", feedback.getTitle());
        fbMap.put("content", feedback.getContent());
        fbMap.put("status", feedback.getStatus());
        fbMap.put("orderId", feedback.getOrderId());
        fbMap.put("createTime", feedback.getCreateTime());

        User u = feedback.getUserId() != null ? userService.getById(feedback.getUserId()) : null;
        fbMap.put("username", u != null ? u.getUsername() : "用户");

        List<FeedbackReply> replies = feedbackReplyService.list(
                new LambdaQueryWrapper<FeedbackReply>()
                        .eq(FeedbackReply::getFeedbackId, feedback.getFeedbackId())
                        .orderByAsc(FeedbackReply::getCreateTime));

        List<Map<String, Object>> replyList = new ArrayList<>();
        for (FeedbackReply r : replies) {
            Map<String, Object> rMap = new HashMap<>();
            rMap.put("replyId", r.getReplyId());
            rMap.put("feedbackId", r.getFeedbackId());
            rMap.put("replyUserId", r.getReplyUserId());
            rMap.put("content", r.getContent());
            rMap.put("createTime", r.getCreateTime());
            User ru = r.getReplyUserId() != null ? userService.getById(r.getReplyUserId()) : null;
            rMap.put("replyUsername", ru != null ? ru.getUsername() : "客服");
            replyList.add(rMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("feedback", fbMap);
        result.put("replies", replyList);
        return APIRes.ok("查询成功", result);
    }

    /**
     * POST /api/v1/feedback/consumer-reply - 消费者追加留言（仅消费者）
     * 请求体：{ feedbackId, replyUserId, content }
     * 发送后将反馈状态重置为0（待回复），通知店员有新消息
     */
    @PostMapping("/consumer-reply")
    @PreAuthorize("hasAnyRole('CONSUMER')")
    public APIRes consumerReply(@RequestBody FeedbackReply reply) {
        if (reply.getFeedbackId() == null) return APIRes.fail(400, "反馈ID不能为空");
        if (StringUtils.isBlank(reply.getContent())) return APIRes.fail(400, "内容不能为空");

        Feedback fb = feedbackService.getById(reply.getFeedbackId());
        if (fb == null) return APIRes.fail(404, "反馈不存在");
        if (fb.getStatus() == 2) return APIRes.fail(400, "该反馈已关闭，无法继续留言");

        reply.setCreateTime(new Date());
        feedbackReplyService.save(reply);

        // 消费者发消息后重置为"未回复"，提示店员跟进
        Feedback updateFb = new Feedback();
        updateFb.setFeedbackId(reply.getFeedbackId());
        updateFb.setStatus(0);
        updateFb.setUpdateTime(new Date());
        feedbackService.updateById(updateFb);

        return APIRes.ok("发送成功", reply);
    }
}
