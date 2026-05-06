package com.concert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @description:    Web MVC配置 - 静态资源映射
 * @author: hzf
 * @date: 2026-05-06
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${concert.upload.path}")
    private String uploadPath;

    @Value("${concert.upload.url-prefix}")
    private String urlPrefix;

    /**
     * 配置静态资源映射，使上传的文件可通过URL直接访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/avatar/** 的请求映射到本地上传目录
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
