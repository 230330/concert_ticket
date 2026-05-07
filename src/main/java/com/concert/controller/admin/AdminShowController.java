package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.ShowRequest;
import com.concert.dto.request.ShowStatusRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Show;
import com.concert.enums.ShowStatus;
import com.concert.exception.BusinessException;
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
        // 业务校验：时间逻辑
        if (request.getSaleEndTime().isAfter(request.getShowTime())) {
            return Result.error("停售时间必须在演出时间之前");
        }
        if (!request.getSaleStartTime().isBefore(request.getSaleEndTime())) {
            return Result.error("停售时间必须晚于开售时间");
        }

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

        // 业务校验：时间逻辑
        if (request.getSaleEndTime().isAfter(request.getShowTime())) {
            return Result.error("停售时间必须在演出时间之前");
        }
        if (!request.getSaleStartTime().isBefore(request.getSaleEndTime())) {
            return Result.error("停售时间必须晚于开售时间");
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
     * 更新场次状态
     *
     * @param id      场次ID
     * @param request 状态变更请求
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody @Validated ShowStatusRequest request) {
        Show show = showService.getById(id);
        if (show == null) {
            throw new BusinessException("场次不存在");
        }

        ShowStatus currentStatus = show.getStatus();
        ShowStatus targetStatus = request.getStatus();

        // 相同状态无需变更
        if (currentStatus == targetStatus) {
            throw new BusinessException("当前状态已是" + currentStatus.getDesc());
        }

        // 校验状态转换合法性
        validateStatusTransition(currentStatus, targetStatus);

        show.setStatus(targetStatus);
        showService.updateById(show);
        return Result.success();
    }

    /**
     * 校验场次状态转换是否合法
     * 合法转换路径：未开售 → 售票中 → 已售罄 → 已结束
     * 未开售/售票中 → 已取消
     * 已结束/已取消为终态，不可再变更
     */
    private void validateStatusTransition(ShowStatus current, ShowStatus target) {
        // 已结束和已取消为终态，不可变更
        if (current == ShowStatus.ENDED) {
            throw new BusinessException("已结束的场次不可变更状态");
        }
        if (current == ShowStatus.CANCELLED) {
            throw new BusinessException("已取消的场次不可变更状态");
        }

        // 任何非终态状态都可以转为已取消
        if (target == ShowStatus.CANCELLED) {
            return;
        }

        // 未开售只能转为售票中
        if (current == ShowStatus.NOT_ON_SALE && target != ShowStatus.ON_SALE) {
            throw new BusinessException("未开售的场次只能变更为售票中或已取消");
        }

        // 售票中只能转为已售罄
        if (current == ShowStatus.ON_SALE && target != ShowStatus.SOLD_OUT) {
            throw new BusinessException("售票中的场次只能变更为已售罄或已取消");
        }

        // 已售罄只能转为已结束
        if (current == ShowStatus.SOLD_OUT && target != ShowStatus.ENDED) {
            throw new BusinessException("已售罄的场次只能变更为已结束或已取消");
        }
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
