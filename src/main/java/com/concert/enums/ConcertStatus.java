package com.concert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @description: 演唱会状态枚举
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public enum ConcertStatus {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束"),
    CANCELLED(3, "已取消");

    @EnumValue
    @JsonValue
    private final int value;

    private final String desc;

    ConcertStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据数值获取枚举
     */
    @JsonCreator
    public static ConcertStatus fromValue(int value) {
        for (ConcertStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的演唱会状态值: " + value);
    }
}
