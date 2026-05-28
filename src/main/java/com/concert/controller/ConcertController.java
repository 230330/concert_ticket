package com.concert.controller;

import com.concert.annotation.RateLimit;
import com.concert.common.Result;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.service.ConcertService;
import com.concert.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * @description: 演出相关接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Tag(name = "演唱会", description = "演唱会搜索、推荐、详情等接口")
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
    @Operation(summary = "热门演出列表", description = "获取热门演唱会分页列表，支持按时间或默认排序")
    @GetMapping("/hot")
    @RateLimit(key = "concert:hot", count = 30, period = 60, limitType = RateLimit.LimitType.IP)
    public Result<PageResponse<ConcertListResponse>> getHotConcerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "default") String sort) {

        int[] params = PageUtil.validate(page, size);
        PageResponse<ConcertListResponse> pageResponse = concertService.getHotConcerts(params[0], params[1], sort);
        return Result.success(pageResponse);
    }

    /**
     * 即将开始演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     */
    @Operation(summary = "即将开始演出列表", description = "获取即将开始的演唱会分页列表")
    @GetMapping("/upcoming")
    public Result<PageResponse<ConcertListResponse>> getUpcomingConcerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        int[] params = PageUtil.validate(page, size);
        PageResponse<ConcertListResponse> pageResponse = concertService.getUpcomingConcerts(params[0], params[1]);
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
    @Operation(summary = "搜索演唱会", description = "支持关键词、城市、艺人名称、日期范围等多条件搜索")
    @GetMapping("/search")
    @RateLimit(key = "concert:search", count = 20, period = 60, limitType = RateLimit.LimitType.IP)
    public Result<PageResponse<ConcertListResponse>> searchConcerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String artistName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        int[] params = PageUtil.validate(page, size);
        PageResponse<ConcertListResponse> pageResponse = concertService.searchConcerts(
                keyword, city, artistName, startDate, endDate, params[0], params[1]);
        return Result.success(pageResponse);
    }

    /**
     * 演出详情（包含艺人信息）
     *
     * @param id 演唱会ID
     */
    @Operation(summary = "演唱会详情", description = "获取演唱会详情，包含关联艺人信息和场次列表")
    @GetMapping("/{id}")
    public Result<ConcertDetailResponse> getConcertDetail(@PathVariable Long id) {
        ConcertDetailResponse response = concertService.getConcertDetail(id);
        return Result.success(response);
    }
}
