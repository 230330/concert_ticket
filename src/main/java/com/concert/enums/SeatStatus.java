package com.concert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @description: 座位表状态枚举
 * @author: hzf
 * @date: 2026年04月28日 11:08
 * @version: 1.0
 */
public enum SeatStatus {
    NOT_STARTED(0, "可用"),
    IN_PROGRESS(1, "占用中"),
    ENDED(2, "锁定中");

    @EnumValue
    @JsonValue
    private final int value;

    private final String desc;

    SeatStatus(int value, String desc) {
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
    public static SeatStatus fromValue(int value) {
        for (SeatStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的座位状态值: " + value);
    }
}
