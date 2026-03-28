package com.concert.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 分页响应
 */
@Data
public class PageResponse<T> {

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页条数
     */
    private Long size;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    public PageResponse() {
    }

    public PageResponse(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size;
        this.records = records;
    }
}
