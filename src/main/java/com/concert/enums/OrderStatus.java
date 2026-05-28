package com.concert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @description: 订单状态枚举
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public enum OrderStatus {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    REFUNDED(3, "已退款"),
    COMPLETED(4, "已完成");

    /**
     * 编译期常量，用于 MyBatis @Select/@Update 注解中引用枚举值，避免硬编码魔法数字
     * 注解属性值必须是编译期常量，无法使用 getValue() 方法调用
     */
    public static final int PENDING_VALUE = 0;
    public static final int PAID_VALUE = 1;
    public static final int CANCELLED_VALUE = 2;
    public static final int REFUNDED_VALUE = 3;
    public static final int COMPLETED_VALUE = 4;

    @EnumValue
    @JsonValue
    private final int value;

    private final String desc;

    OrderStatus(int value, String desc) {
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
    public static OrderStatus fromValue(int value) {
        for (OrderStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态值: " + value);
    }
}
