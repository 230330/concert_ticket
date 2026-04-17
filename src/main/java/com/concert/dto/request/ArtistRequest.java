package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @description:    艺人请求参数
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class ArtistRequest {

    /**
     * 艺人名称
     */
    @NotBlank(message = "艺人名称不能为空")
    @Size(max = 50, message = "艺人名称最多50个字符")
    private String name;

    /**
     * 艺人头像URL
     */
    private String avatar;

    /**
     * 艺人简介
     */
    private String description;
}
