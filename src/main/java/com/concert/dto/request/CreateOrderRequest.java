package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {

    /**
     * 场次ID
     */
    @NotNull(message = "场次ID不能为空")
    private Long showId;

    /**
     * 票档ID
     */
    @NotNull(message = "票档ID不能为空")
    private Long ticketTypeId;

    /**
     * 座位ID列表
     */
    @NotEmpty(message = "请选择座位")
    private List<Long> seatIds;
}
