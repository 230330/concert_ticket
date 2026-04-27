package com.concert.controller;

import com.concert.BaseIntegrationTest;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 单元测试
 * 使用 MockMvc 进行 Controller 层的 HTTP 接口测试
 */
@AutoConfigureMockMvc
class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterWithValidData() throws Exception {
        // 准备测试数据
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        request.setPassword("Test123456");
        request.setCode("123456"); // 注意：实际测试需要真实的验证码

        // 执行请求并验证响应
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testLoginWithValidCredentials() throws Exception {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test123456");

        // 执行请求并验证响应
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    void testRegisterWithInvalidPhone() throws Exception {
        // 准备测试数据 - 无效手机号
        RegisterRequest request = new RegisterRequest();
        request.setPhone("invalid");
        request.setPassword("Test123456");
        request.setCode("123456");

        // 执行请求并验证返回验证错误
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterWithShortPassword() throws Exception {
        // 准备测试数据 - 密码太短
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        request.setPassword("123"); // 密码长度不足
        request.setCode("123456");

        // 执行请求并验证返回验证错误
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
