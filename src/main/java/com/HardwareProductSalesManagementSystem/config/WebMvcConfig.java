package com.HardwareProductSalesManagementSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.avatar.path}")
    private String avatarUploadPath;
    @Value("${upload.avatar.url}")
    private String avatarAccessUrl;

    @Value("${upload.hardware.path:D:/Project/毕业设计/HardwareProductSalesManagementSystem/upload/images/hardware/}")
    private String hardwareUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler(avatarAccessUrl)
                .addResourceLocations("file:" + avatarUploadPath);

        registry.addResourceHandler("/images/product/**")
                .addResourceLocations("file:" + hardwareUploadPath);
    }

    /** 允许跨域（前后端分离时使用） */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
