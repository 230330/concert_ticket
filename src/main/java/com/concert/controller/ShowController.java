package com.concert.controller;

import com.concert.common.Result;
import com.concert.dto.response.SeatMapResponse;
import com.concert.dto.response.ShowListResponse;
import com.concert.service.ShowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * @description:    场次相关接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/show")
public class ShowController {

    @Resource
    private ShowService showService;

    /**
     * 根据演出ID查询场次列表
     *
     * @param concertId 演出ID
     * @param date      可选日期筛选
     */
    @GetMapping("/list")
    public Result<List<ShowListResponse>> getShowList(
            @RequestParam Long concertId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        List<ShowListResponse> responseList = showService.getShowList(concertId, date);
        return Result.success(responseList);
    }

    /**
     * 获取场次座位图
     *
     * @param showId 场次ID
     */
    @GetMapping("/{showId}/seats")
    public Result<SeatMapResponse> getSeatMap(@PathVariable Long showId) {
        SeatMapResponse response = showService.getSeatMap(showId);
        return Result.success(response);
    }
}
