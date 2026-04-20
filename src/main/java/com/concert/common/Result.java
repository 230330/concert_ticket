package com.concert.common;

import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @description: 统一响应结果
 * @author: hzf
 * @date: 2026-04-17 15:21
 * @return:
 *
 * */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    /**
     * 成功（带数据）
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功（带消息和数据）
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_, _ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    /**
     * 失败（带状态码）
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_, _ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 失败（带状态码，消息，数据）
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_, _, _ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_, _, _ -> new", pure = true)
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 未认证
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message);
    }

    /**
     * 无权限
     * @NotNull：表示返回值一定不是 null，可以放心使用而无需判空。
     * @Contract("_ -> new")：每次调用都会创建新对象；输入参数不影响返回对象的“新建性”。
     * pure = true：表示该方法是一个纯函数，即输入参数相同，输出结果一定相同，并且没有副作用。
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message);
    }
}
