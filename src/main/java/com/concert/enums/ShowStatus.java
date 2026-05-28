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

    /**
     * 编译期常量，用于 MyBatis @Select/@Update 注解中引用枚举值，避免硬编码魔法数字
     */
    public static final int NOT_ON_SALE_VALUE = 0;
    public static final int ON_SALE_VALUE = 1;
    public static final int SOLD_OUT_VALUE = 2;
    public static final int ENDED_VALUE = 3;
    public static final int CANCELLED_VALUE = 4;

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
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ShowStatus fromValue(int value) {
        for (ShowStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的场次状态值: " + value);
    }
}
