package com.concert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @description: 用户状态枚举
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public enum UserStatus {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    @EnumValue
    @JsonValue
    private final int value;

    private final String desc;

    UserStatus(int value, String desc) {
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
    public static UserStatus fromValue(int value) {
        for (UserStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的用户状态值: " + value);
    }
}
