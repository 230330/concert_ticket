package com.concert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @description: 场次状态枚举
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public enum ShowStatus {

    NOT_ON_SALE(0, "未开售"),
    ON_SALE(1, "售票中"),
    SOLD_OUT(2, "已售罄"),
    ENDED(3, "已结束"),
    CANCELLED(4, "已取消");

    @EnumValue
    @JsonValue
    private final int value;

    private final String desc;

    ShowStatus(int value, String desc) {
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
    public static ShowStatus fromValue(int value) {
        for (ShowStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的场次状态值: " + value);
    }
}
