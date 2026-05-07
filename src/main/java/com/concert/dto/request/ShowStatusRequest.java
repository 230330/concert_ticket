package com.concert.dto.request;

import com.concert.enums.ShowStatus;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description:    场次状态变更请求参数
 * @author: hzf
 * @date: 2026-05-07
 */
@Data
public class ShowStatusRequest {

    /**
     * 目标状态
     */
    @NotNull(message = "目标状态不能为空")
    private ShowStatus status;
}
