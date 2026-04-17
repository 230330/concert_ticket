package com.concert.controller;

import com.concert.common.Result;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.service.ConcertService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 演唱会控制器
 */
@RestController
@RequestMapping("/api/concert")
public class ConcertController {

    @Resource
    private ConcertService concertService;

    /**
     * 热门演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     * @param sort 排序方式：time-按时间，default-默认
     */
    @GetMapping("/hot")
    public Result<PageResponse<ConcertListResponse>> getHotConcerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "default") String sort) {

        PageResponse<ConcertListResponse> pageResponse = concertService.getHotConcerts(page, size, sort);
        return Result.success(pageResponse);
    }

    /**
     * 即将开始演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     */
    @GetMapping("/upcoming")
    public Result<PageResponse<ConcertListResponse>> getUpcomingConcerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        PageResponse<ConcertListResponse> pageResponse = concertService.getUpcomingConcerts(page, size);
        return Result.success(pageResponse);
    }

    /**
     * 搜索演唱会（支持关键词、城市、艺人、日期范围筛选）
     *
     * @param keyword    关键词（模糊匹配演唱会名称）
     * @param city       城市（精确匹配场馆所在城市）
     * @param artistName 艺人名称（模糊匹配）
     * @param startDate  场次开始日期（可选，yyyy-MM-dd）
     * @param endDate    场次结束日期（可选，yyyy-MM-dd）
     * @param page       页码
     * @param size       每页条数
     */
    @GetMapping("/search")
    public Result<PageResponse<ConcertListResponse>> searchConcerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String artistName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        PageResponse<ConcertListResponse> pageResponse = concertService.searchConcerts(
                keyword, city, artistName, startDate, endDate, page, size);
        return Result.success(pageResponse);
    }

    /**
     * 演出详情（包含艺人信息）
     *
     * @param id 演唱会ID
     */
    @GetMapping("/{id}")
    public Result<ConcertDetailResponse> getConcertDetail(@PathVariable Long id) {
        ConcertDetailResponse response = concertService.getConcertDetail(id);
        return Result.success(response);
    }
}
