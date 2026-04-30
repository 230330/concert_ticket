package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.ShowRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Show;
import com.concert.enums.ShowStatus;
import com.concert.service.ShowService;
import com.concert.utils.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:    管理员-场次管理
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/admin/show")
public class AdminShowController {

    @Resource
    private ShowService showService;

    /**
     * 分页查询场次列表
     *
     * @param page      页码
     * @param size      每页条数
     * @param concertId 演唱会ID筛选
     * @param status    状态筛选
     */
    @GetMapping("/list")
    public Result<PageResponse<Show>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) Integer status) {

        int[] params = PageUtil.validate(page, size);

        LambdaQueryWrapper<Show> queryWrapper = new LambdaQueryWrapper<>();
        if (concertId != null) {
            queryWrapper.eq(Show::getConcertId, concertId);
        }
        if (status != null) {
            queryWrapper.eq(Show::getStatus, status);
        }
        queryWrapper.orderByDesc(Show::getId);

        Page<Show> showPage = new Page<>(params[0], params[1]);
        showService.page(showPage, queryWrapper);

        PageResponse<Show> pageResponse = new PageResponse<>(
                showPage.getCurrent(),
                showPage.getSize(),
                showPage.getTotal(),
                showPage.getRecords()
        );
        return Result.success(pageResponse);
    }

    /**
     * 查询场次详情
     *
     * @param id 场次ID
     */
    @GetMapping("/{id}")
    public Result<Show> detail(@PathVariable Long id) {
        Show show = showService.getById(id);
        if (show == null) {
            return Result.error("场次不存在");
        }
        return Result.success(show);
    }

    /**
     * 新增场次
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated ShowRequest request) {
        Show show = new Show();
        show.setConcertId(request.getConcertId());
        show.setVenueId(request.getVenueId());
        show.setShowTime(request.getShowTime());
        show.setSaleStartTime(request.getSaleStartTime());
        show.setSaleEndTime(request.getSaleEndTime());
        show.setStatus(request.getStatus() != null ? request.getStatus() : ShowStatus.NOT_ON_SALE);

        showService.save(show);
        return Result.success();
    }

    /**
     * 更新场次
     *
     * @param id 场次ID
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated ShowRequest request) {
        Show show = showService.getById(id);
        if (show == null) {
            return Result.error("场次不存在");
        }

        show.setConcertId(request.getConcertId());
        show.setVenueId(request.getVenueId());
        show.setShowTime(request.getShowTime());
        show.setSaleStartTime(request.getSaleStartTime());
        show.setSaleEndTime(request.getSaleEndTime());
        if (request.getStatus() != null) {
            show.setStatus(request.getStatus());
        }

        showService.updateById(show);
        return Result.success();
    }

    /**
     * 删除场次
     *
     * @param id 场次ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Show show = showService.getById(id);
        if (show == null) {
            return Result.error("场次不存在");
        }

        showService.removeById(id);
        return Result.success();
    }
}
