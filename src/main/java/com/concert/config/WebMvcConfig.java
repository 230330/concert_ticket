package com.concert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

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
     * 将相对路径基于user.dir解析为绝对路径，与FileUploadController保持一致
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            String projectRoot = System.getProperty("user.dir");
            dir = new File(projectRoot, uploadPath);
        }
        String absolutePath = dir.getAbsolutePath();

        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
