package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 票种创建/更新请求
 */
@Data
public class TicketTypeRequest {

    /**
     * 场次ID
     */
    @NotNull(message = "场次ID不能为空")
    private Long showId;

    /**
     * 区域ID
     */
    private Long areaId;

    /**
     * 票档名称
     */
    @NotBlank(message = "票档名称不能为空")
    private String name;

    /**
     * 票价
     */
    @NotNull(message = "票价不能为空")
    private BigDecimal price;

    /**
     * 总库存
     */
    @NotNull(message = "总库存不能为空")
    private Integer totalStock;

    /**
     * 可用库存
     */
    private Integer availableStock;
}
