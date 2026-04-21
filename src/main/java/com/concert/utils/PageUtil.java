package com.concert.utils;

import com.concert.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @description:    分页参数校验工具
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
/**
 * 新增一个测试注释*/

@Component
public class PageUtil {

    /**
     * 分页大小上限
     */
    private static int maxSize;

    /**
     * 分页大小默认值
     */
    private static int defaultSize;

    @Value("${concert.page.max-size:100}")
    public void setMaxSize(int maxSize) {
        PageUtil.maxSize = maxSize;
    }

    @Value("${concert.page.default-size:10}")
    public void setDefaultSize(int defaultSize) {
        PageUtil.defaultSize = defaultSize;
    }

    /**
     * 校验并规范化分页参数
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 规范化后的 [page, size] 数组
     */
    public static int[] validate(Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = defaultSize;
        }
        if (size > maxSize) {
            throw new BusinessException("每页条数不能超过" + maxSize);
        }
        return new int[]{page, size};
    }

    /**
     * 校验并规范化分页参数（不抛异常，自动截断）
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 规范化后的 [page, size] 数组
     */
    public static int[] normalize(Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = defaultSize;
        }
        if (size > maxSize) {
            size = maxSize;
        }
        return new int[]{page, size};
    }

    public static int getMaxSize() {
        return maxSize;
    }

    public static int getDefaultSize() {
        return defaultSize;
    }
}
