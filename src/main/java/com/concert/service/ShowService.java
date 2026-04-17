package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.dto.response.SeatMapResponse;
import com.concert.dto.response.ShowListResponse;
import com.concert.entity.Show;

import java.time.LocalDate;
import java.util.List;

/**
 * 场次服务接口
 */
public interface ShowService extends IService<Show> {

    /**
     * 根据演出ID查询场次列表
     *
     * @param concertId 演出ID
     * @param date      可选日期筛选
     * @return 场次列表响应
     */
    List<ShowListResponse> getShowList(Long concertId, LocalDate date);

    /**
     * 获取场次座位图
     *
     * @param showId 场次ID
     * @return 座位图响应
     */
    SeatMapResponse getSeatMap(Long showId);
}
