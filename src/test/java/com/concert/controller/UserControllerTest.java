package com.concert.controller;

import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.response.LoginResponse;
import com.concert.exception.GlobalExceptionHandler;
import com.concert.service.SmsVerificationCodeService;
import com.concert.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 单元测试
 *
 * <p>只装配 Controller 与全局异常处理器，业务依赖全部 mock，不加载 Spring Boot 自动配置，
 * 因此不依赖 MySQL、Redis、MyBatis 等外部环境（早先版本因为依赖这些环境而长期无法通过）。
 * 请求参数校验与统一响应格式在此验证。
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {UserController.class, GlobalExceptionHandler.class,
        UserControllerTest.TestConfig.class})
class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 被测 Controller 所依赖的协作对象
     */
    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        SmsVerificationCodeService smsVerificationCodeService() {
            return mock(SmsVerificationCodeService.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }
    }

    @Test
    @DisplayName("合法凭据登录返回 Token")
    void testLoginWithValidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("Test123456");

        when(userService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("test_token", 3600L));

        // When & Then
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("test_token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("合法数据注册成功")
    void testRegisterWithValidData() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        request.setPassword("Test123456");
        request.setCode("123456");

        // When & Then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("手机号格式非法返回校验错误")
    void testRegisterWithInvalidPhone() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhone("invalid");
        request.setPassword("Test123456");
        request.setCode("123456");

        // When & Then
        // 参数校验失败由 GlobalExceptionHandler 统一处理，返回 HTTP 200 + 响应体 code=400
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("密码长度不足返回校验错误")
    void testRegisterWithShortPassword() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        request.setPassword("123");
        request.setCode("123456");

        // When & Then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
