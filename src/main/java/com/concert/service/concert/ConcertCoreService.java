package com.concert.service.concert;

import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;

/**
 * @description: 演唱会核心服务（CRUD + 详情）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface ConcertCoreService {

    /**
     * 演唱会详情
     *
     * @param id 演唱会ID
     * @return 详情响应
     */
    ConcertDetailResponse getConcertDetail(Long id);
}
