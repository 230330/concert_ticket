package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.StockAdjustRequest;
import com.concert.dto.request.TicketTypeRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.TicketType;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.TicketTypeService;
import com.concert.utils.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-票种和库存管理控制器
 */
@RestController
@RequestMapping("/api/admin/ticket-type")
public class AdminTicketTypeController {

    @Resource
    private TicketTypeService ticketTypeService;

    /**
     * 分页查询票种列表
     *
     * @param page   页码
     * @param size   每页条数
     * @param showId 场次ID筛选
     */
    @GetMapping("/list")
    public Result<PageResponse<TicketType>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long showId) {

        int[] params = PageUtil.validate(page, size);

        LambdaQueryWrapper<TicketType> queryWrapper = new LambdaQueryWrapper<>();
        if (showId != null) {
            queryWrapper.eq(TicketType::getShowId, showId);
        }
        queryWrapper.orderByDesc(TicketType::getId);

        Page<TicketType> ticketTypePage = new Page<>(params[0], params[1]);
        ticketTypeService.page(ticketTypePage, queryWrapper);

        PageResponse<TicketType> pageResponse = new PageResponse<>(
                ticketTypePage.getCurrent(),
                ticketTypePage.getSize(),
                ticketTypePage.getTotal(),
                ticketTypePage.getRecords()
        );
        return Result.success(pageResponse);
    }

    /**
     * 查询票种详情
     *
     * @param id 票种ID
     */
    @GetMapping("/{id}")
    public Result<TicketType> detail(@PathVariable Long id) {
        TicketType ticketType = ticketTypeService.getById(id);
        if (ticketType == null) {
            return Result.error("票种不存在");
        }
        return Result.success(ticketType);
    }

    /**
     * 新增票种
     */
    @PostMapping
    public Result<Void> add(@RequestBody @Validated TicketTypeRequest request) {
        TicketType ticketType = new TicketType();
        ticketType.setShowId(request.getShowId());
        ticketType.setAreaId(request.getAreaId());
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalStock(request.getTotalStock());
        // 可用库存默认等于总库存
        ticketType.setAvailableStock(request.getAvailableStock() != null ? request.getAvailableStock() : request.getTotalStock());

        ticketTypeService.save(ticketType);
        return Result.success();
    }

    /**
     * 更新票种
     *
     * @param id 票种ID
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated TicketTypeRequest request) {
        TicketType ticketType = ticketTypeService.getById(id);
        if (ticketType == null) {
            return Result.error("票种不存在");
        }

        ticketType.setShowId(request.getShowId());
        ticketType.setAreaId(request.getAreaId());
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalStock(request.getTotalStock());
        if (request.getAvailableStock() != null) {
            ticketType.setAvailableStock(request.getAvailableStock());
        }

        ticketTypeService.updateById(ticketType);
        return Result.success();
    }

    /**
     * 删除票种
     *
     * @param id 票种ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        TicketType ticketType = ticketTypeService.getById(id);
        if (ticketType == null) {
            return Result.error("票种不存在");
        }

        // 如果有已售出但未完成的票，不允许删除
        if (ticketType.getAvailableStock() < ticketType.getTotalStock()) {
            return Result.error("该票种存在未完成的订单，无法删除");
        }

        ticketTypeService.removeById(id);
        return Result.success();
    }

    /**
     * 库存调整（增减库存）
     */
    @PutMapping("/stock/adjust")
    public Result<TicketType> adjustStock(@RequestBody @Validated StockAdjustRequest request) {
        TicketType ticketType = ticketTypeService.getById(request.getTicketTypeId());
        if (ticketType == null) {
            throw new NotFoundException("票种不存在");
        }

        int adjustQuantity = request.getAdjustQuantity();
        if (adjustQuantity == 0) {
            throw new BusinessException("调整数量不能为0");
        }

        if (adjustQuantity > 0) {
            // 增加库存
            LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TicketType::getId, ticketType.getId())
                    .setSql("available_stock = available_stock + " + adjustQuantity)
                    .setSql("total_stock = total_stock + " + adjustQuantity);
            ticketTypeService.update(updateWrapper);
        } else {
            // 减少库存（确保不超减）
            int absQuantity = Math.abs(adjustQuantity);
            LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(TicketType::getId, ticketType.getId())
                    .ge(TicketType::getAvailableStock, absQuantity)
                    .setSql("available_stock = available_stock - " + absQuantity)
                    .setSql("total_stock = total_stock - " + absQuantity);
            boolean updated = ticketTypeService.update(updateWrapper);
            if (!updated) {
                throw new BusinessException("可用库存不足，无法减少");
            }
        }

        return Result.success(ticketTypeService.getById(request.getTicketTypeId()));
    }
}
