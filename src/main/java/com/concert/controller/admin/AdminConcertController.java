package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.ConcertRequest;
import com.concert.dto.request.ConcertStatusRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Concert;
import com.concert.entity.ConcertArtist;
import com.concert.enums.ConcertStatus;
import com.concert.exception.BusinessException;
import com.concert.service.ConcertArtistService;
import com.concert.service.ConcertService;
import com.concert.utils.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:    管理员演唱会管理控制器
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/admin/concert")
public class AdminConcertController {

    @Resource
    private ConcertService concertService;

    @Resource
    private ConcertArtistService concertArtistService;

    /**
     * 分页查询演唱会列表
     *
     * @param page    页码
     * @param size    每页条数
     * @param name    演唱会名称（模糊搜索）
     * @param status  状态筛选
     */
    @GetMapping("/list")
    public Result<PageResponse<Concert>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {

        int[] params = PageUtil.validate(page, size);

        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like(Concert::getName, name.trim());
        }
        if (status != null) {
            queryWrapper.eq(Concert::getStatus, status);
        }
        queryWrapper.orderByDesc(Concert::getId);

        Page<Concert> concertPage = new Page<>(params[0], params[1]);
        concertService.page(concertPage, queryWrapper);

        PageResponse<Concert> pageResponse = new PageResponse<>(
                concertPage.getCurrent(),
                concertPage.getSize(),
                concertPage.getTotal(),
                concertPage.getRecords()
        );
        return Result.success(pageResponse);
    }

    /**
     * 查询演唱会详情
     *
     * @param id 演唱会ID
     */
    @GetMapping("/{id}")
    public Result<Concert> detail(@PathVariable Long id) {
        Concert concert = concertService.getById(id);
        if (concert == null) {
            return Result.error("演唱会不存在");
        }
        return Result.success(concert);
    }

    /**
     * 新增演唱会
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated ConcertRequest request) {
        Concert concert = new Concert();
        concert.setName(request.getName());
        concert.setPoster(request.getPoster());
        concert.setDescription(request.getDescription());
        concert.setStatus(request.getStatus() != null ? request.getStatus() : ConcertStatus.NOT_STARTED);

        concertService.save(concert);

        // 保存艺人关联
        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            concertArtistService.saveConcertArtists(concert.getId(), request.getArtistIds());
        }

        return Result.success();
    }

    /**
     * 更新演唱会
     *
     * @param id 演唱会ID
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated ConcertRequest request) {
        Concert concert = concertService.getById(id);
        if (concert == null) {
            return Result.error("演唱会不存在");
        }

        concert.setName(request.getName());
        concert.setPoster(request.getPoster());
        concert.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            concert.setStatus(request.getStatus());
        }

        concertService.updateById(concert);

        // 更新艺人关联：先删后增
        if (request.getArtistIds() != null) {
            LambdaQueryWrapper<ConcertArtist> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(ConcertArtist::getConcertId, id);
            concertArtistService.remove(deleteWrapper);

            concertArtistService.saveConcertArtists(id, request.getArtistIds());
        }

        return Result.success();
    }

    /**
     * 更新演唱会状态
     *
     * @param id      演唱会ID
     * @param request 状态变更请求
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody @Validated ConcertStatusRequest request) {
        Concert concert = concertService.getById(id);
        if (concert == null) {
            throw new BusinessException("演唱会不存在");
        }

        ConcertStatus currentStatus = concert.getStatus();
        ConcertStatus targetStatus = request.getStatus();

        // 相同状态无需变更
        if (currentStatus == targetStatus) {
            throw new BusinessException("当前状态已是" + currentStatus.getDesc());
        }

        // 校验状态转换合法性
        validateStatusTransition(currentStatus, targetStatus);

        concert.setStatus(targetStatus);
        concertService.updateById(concert);
        return Result.success();
    }

    /**
     * 校验演唱会状态转换是否合法
     * 合法转换路径：未开始 → 进行中 → 已结束
     * 任何状态 → 已取消
     * 已取消/已结束 为终态，不可再变更
     */
    private void validateStatusTransition(ConcertStatus current, ConcertStatus target) {
        // 已结束和已取消为终态，不可变更
        if (current == ConcertStatus.ENDED) {
            throw new BusinessException("已结束的演唱会不可变更状态");
        }
        if (current == ConcertStatus.CANCELLED) {
            throw new BusinessException("已取消的演唱会不可变更状态");
        }

        // 任何非终态状态都可以转为已取消
        if (target == ConcertStatus.CANCELLED) {
            return;
        }

        // 未开始只能转为进行中
        if (current == ConcertStatus.NOT_STARTED && target != ConcertStatus.IN_PROGRESS) {
            throw new BusinessException("未开始的演唱会只能变更为进行中或已取消");
        }

        // 进行中只能转为已结束
        if (current == ConcertStatus.IN_PROGRESS && target != ConcertStatus.ENDED) {
            throw new BusinessException("进行中的演唱会只能变更为已结束或已取消");
        }
    }

    /**
     * 删除演唱会
     *
     * @param id 演唱会ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Concert concert = concertService.getById(id);
        if (concert == null) {
            return Result.error("演唱会不存在");
        }

        // 删除艺人关联
        LambdaQueryWrapper<ConcertArtist> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ConcertArtist::getConcertId, id);
        concertArtistService.remove(deleteWrapper);

        // 删除演唱会
        concertService.removeById(id);

        return Result.success();
    }
}
