package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

/**
 * 用户信息更新请求
 */
@Data
public class UserUpdateRequest {

    /**
     * 昵称
     */
    @Size(max = 30, message = "昵称最多30个字符")
    private String nickname;

    /**
     * 头像URL
     */
    @Size(max = 500, message = "头像URL最多500个字符")
    private String avatar;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;
}
