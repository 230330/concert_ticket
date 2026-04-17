package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 演唱会创建/更新请求
 */
@Data
public class ConcertRequest {

    /**
     * 演唱会名称
     */
    @NotBlank(message = "演唱会名称不能为空")
    @Size(max = 100, message = "演唱会名称最多100个字符")
    private String name;

    /**
     * 演唱会海报URL
     */
    private String poster;

    /**
     * 演唱会描述
     */
    private String description;

    /**
     * 状态：0-未开始，1-进行中，2-已结束，3-已取消
     */
    private Integer status;

    /**
     * 关联艺人ID列表
     */
    private List<Long> artistIds;
}
