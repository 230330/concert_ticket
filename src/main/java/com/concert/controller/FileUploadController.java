package com.concert.controller;

import com.concert.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @description:    文件上传控制器
 * @author: hzf
 * @date: 2026-05-06
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${concert.upload.path}")
    private String uploadPath;

    @Value("${concert.upload.url-prefix}")
    private String urlPrefix;

    /**
     * 允许的图片格式
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");

    /**
     * 最大文件大小：10MB
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 上传头像
     * @param file 图片文件
     * @return 头像访问URL
     */
    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过10MB");
        }

        // 3. 校验文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.error("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("仅支持JPG和PNG格式的图片");
        }

        // 4. 校验文件Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持上传图片文件");
        }

        // 5. 生成唯一文件名
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        // 6. 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                log.error("创建上传目录失败: {}", uploadPath);
                return Result.error("文件上传失败，请稍后重试");
            }
        }

        // 7. 保存文件
        File destFile = new File(uploadDir, newFilename);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            return Result.error("文件上传失败，请稍后重试");
        }

        // 8. 返回可访问的URL
        String avatarUrl = urlPrefix + "/" + newFilename;
        return Result.success(avatarUrl);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
