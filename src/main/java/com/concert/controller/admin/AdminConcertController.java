package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.ConcertRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Concert;
import com.concert.entity.ConcertArtist;
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
    @PostMapping
    public Result<Void> add(@RequestBody @Validated ConcertRequest request) {
        Concert concert = new Concert();
        concert.setName(request.getName());
        concert.setPoster(request.getPoster());
        concert.setDescription(request.getDescription());
        concert.setStatus(request.getStatus() != null ? request.getStatus() : 0);

        concertService.save(concert);

        // 保存艺人关联
        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            saveConcertArtists(concert.getId(), request.getArtistIds());
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

            saveConcertArtists(id, request.getArtistIds());
        }

        return Result.success();
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

    /**
     * 保存演唱会-艺人关联
     */
    private void saveConcertArtists(Long concertId, List<Long> artistIds) {
        List<ConcertArtist> concertArtists = artistIds.stream().map(artistId -> {
            ConcertArtist ca = new ConcertArtist();
            ca.setConcertId(concertId);
            ca.setArtistId(artistId);
            return ca;
        }).collect(Collectors.toList());
        concertArtistService.saveBatch(concertArtists);
    }
}
