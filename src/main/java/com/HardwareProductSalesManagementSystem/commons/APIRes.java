package com.HardwareProductSalesManagementSystem.commons;

import lombok.Data;

@Data
public class APIRes {
    private int code; //状态码
    private String msg; //响应消息
    private Object data; //响应对象

    // 修复：msg参数生效，不再硬编码"成功"
    public static APIRes ok(String msg, Object data) {
        APIRes res = new APIRes();
        res.setCode(200);
        res.setMsg(msg);
        res.setData(data);
        return res;
    }

    // 重载：无数据的成功响应
    public static APIRes ok(String msg) {
        return ok(msg, null);
    }

    // 修复：code参数生效，不再硬编码500
    public static APIRes fail(int code, String msg) {
        APIRes res = new APIRes();
        res.setCode(code);
        res.setMsg(msg);
        res.setData(null);
        return res;
    }

    // 重载：默认500的失败响应（兼容原有调用）
    public static APIRes fail(String msg) {
        return fail(500, msg);
    }
}