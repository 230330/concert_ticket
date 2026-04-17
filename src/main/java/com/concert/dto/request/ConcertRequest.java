package com.concert.dto.request;

import com.concert.enums.ConcertStatus;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @description:    演唱会请求参数
 * @author: hzf
 * @date: 2026-04-17 15:30
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
     * 状态
     */
    private ConcertStatus status;

    /**
     * 关联艺人ID列表
     */
    private List<Long> artistIds;
}
