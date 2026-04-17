package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.ArtistRequest;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Artist;
import com.concert.service.ArtistService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-艺人管理控制器
 */
@RestController
@RequestMapping("/api/admin/artist")
public class AdminArtistController {

    @Resource
    private ArtistService artistService;

    /**
     * 分页查询艺人列表
     *
     * @param page 页码
     * @param size 每页条数
     * @param name 艺人名称（模糊搜索）
     */
    @GetMapping("/list")
    public Result<PageResponse<Artist>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {

        LambdaQueryWrapper<Artist> queryWrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like(Artist::getName, name.trim());
        }
        queryWrapper.orderByDesc(Artist::getId);

        Page<Artist> artistPage = new Page<>(page, size);
        artistService.page(artistPage, queryWrapper);

        PageResponse<Artist> pageResponse = new PageResponse<>(
                artistPage.getCurrent(),
                artistPage.getSize(),
                artistPage.getTotal(),
                artistPage.getRecords()
        );
        return Result.success(pageResponse);
    }

    /**
     * 查询艺人详情
     *
     * @param id 艺人ID
     */
    @GetMapping("/{id}")
    public Result<Artist> detail(@PathVariable Long id) {
        Artist artist = artistService.getById(id);
        if (artist == null) {
            return Result.error("艺人不存在");
        }
        return Result.success(artist);
    }

    /**
     * 新增艺人
     */
    @PostMapping
    public Result<Void> add(@RequestBody @Validated ArtistRequest request) {
        Artist artist = new Artist();
        artist.setName(request.getName());
        artist.setAvatar(request.getAvatar());
        artist.setDescription(request.getDescription());

        artistService.save(artist);
        return Result.success();
    }

    /**
     * 更新艺人
     *
     * @param id 艺人ID
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Validated ArtistRequest request) {
        Artist artist = artistService.getById(id);
        if (artist == null) {
            return Result.error("艺人不存在");
        }

        artist.setName(request.getName());
        artist.setAvatar(request.getAvatar());
        artist.setDescription(request.getDescription());

        artistService.updateById(artist);
        return Result.success();
    }

    /**
     * 删除艺人
     *
     * @param id 艺人ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Artist artist = artistService.getById(id);
        if (artist == null) {
            return Result.error("艺人不存在");
        }

        artistService.removeById(id);
        return Result.success();
    }
}
