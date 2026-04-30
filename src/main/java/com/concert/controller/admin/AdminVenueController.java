package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.VenueRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Venue;
import com.concert.service.VenueService;
import com.concert.utils.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:    管理员场馆管理控制器
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/admin/venue")
public class AdminVenueController {

    @Resource
    private VenueService venueService;

    /**
     * 分页查询场馆列表
     *
     * @param page 页码
     * @param size 每页条数
     * @param name 场馆名称（模糊搜索）
     * @param city 城市筛选
     */
    @GetMapping("/list")
    public Result<PageResponse<Venue>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city) {

        int[] params = PageUtil.validate(page, size);

        LambdaQueryWrapper<Venue> queryWrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like(Venue::getName, name.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            queryWrapper.eq(Venue::getCity, city.trim());
        }
        queryWrapper.orderByDesc(Venue::getId);

        Page<Venue> venuePage = new Page<>(params[0], params[1]);
        venueService.page(venuePage, queryWrapper);

        PageResponse<Venue> pageResponse = new PageResponse<>(
                venuePage.getCurrent(),
                venuePage.getSize(),
                venuePage.getTotal(),
                venuePage.getRecords()
        );
        return Result.success(pageResponse);
    }

    /**
     * 查询场馆详情
     *
     * @param id 场馆ID
     */
    @GetMapping("/{id}")
    public Result<Venue> detail(@PathVariable Long id) {
        Venue venue = venueService.getById(id);
        if (venue == null) {
            return Result.error("场馆不存在");
        }
        return Result.success(venue);
    }

    /**
     * 新增场馆
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated VenueRequest request) {
        Venue venue = new Venue();
        venue.setName(request.getName());
        venue.setCity(request.getCity());
        venue.setAddress(request.getAddress());
        venue.setCapacity(request.getCapacity());

        venueService.save(venue);
        return Result.success();
    }

    /**
     * 更新场馆
     *
     * @param id 场馆ID
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated VenueRequest request) {
        Venue venue = venueService.getById(id);
        if (venue == null) {
            return Result.error("场馆不存在");
        }

        venue.setName(request.getName());
        venue.setCity(request.getCity());
        venue.setAddress(request.getAddress());
        venue.setCapacity(request.getCapacity());

        venueService.updateById(venue);
        return Result.success();
    }

    /**
     * 删除场馆
     *
     * @param id 场馆ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Venue venue = venueService.getById(id);
        if (venue == null) {
            return Result.error("场馆不存在");
        }

        venueService.removeById(id);
        return Result.success();
    }
}
