package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 库存调整请求
 */
@Data
public class StockAdjustRequest {

    /**
     * 票种ID
     */
    @NotNull(message = "票种ID不能为空")
    private Long ticketTypeId;

    /**
     * 调整数量（正数增加，负数减少）
     */
    @NotNull(message = "调整数量不能为空")
    private Integer adjustQuantity;

    /**
     * 调整原因
     */
    private String reason;
}
