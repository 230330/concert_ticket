package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 场馆创建/更新请求
 */
@Data
public class VenueRequest {

    /**
     * 场馆名称
     */
    @NotBlank(message = "场馆名称不能为空")
    @Size(max = 100, message = "场馆名称最多100个字符")
    private String name;

    /**
     * 所在城市
     */
    @NotBlank(message = "所在城市不能为空")
    private String city;

    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    private String address;

    /**
     * 场馆容量
     */
    @NotNull(message = "场馆容量不能为空")
    private Integer capacity;
}
