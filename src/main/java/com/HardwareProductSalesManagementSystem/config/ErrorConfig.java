package com.HardwareProductSalesManagementSystem.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 全局错误页面配置：处理404/500等错误码
 */
@Configuration
public class ErrorConfig implements ErrorPageRegistrar {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        // 1. 配置404错误页：匹配404状态码，跳转到/404.html
        ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/404.html");
        // 2. 可选：配置500错误页（按需添加）
        // ErrorPage error500Page = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500.html");

        // 注册错误页
        registry.addErrorPages(error404Page);
        // registry.addErrorPages(error500Page);
    }
}