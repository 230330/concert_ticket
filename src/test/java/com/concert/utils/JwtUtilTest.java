package com.concert.utils;

import com.concert.BaseIntegrationTest;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
class JwtUtilTest extends BaseIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGenerateAndValidateToken() {
        // 给定
        Long userId = 1L;
        String phone = "13800138000";

        // 当
        String token = jwtUtil.generateToken(userId, phone);

        // 则
        assertNotNull(token, "生成的 Token 不应为 null");
        assertTrue(token.length() > 0, "Token 长度应大于 0");
    }


    @Test
    void testGetUserIdFromToken() {
        // 给定
        Long userId = 1L;
        String phone = "13800138000";
        String token = jwtUtil.generateToken(userId, phone);

        // 当
        Long extractedUserId = jwtUtil.getUserId(token);

        // 则
        assertNotNull(extractedUserId, "从 Token 中提取的 UserId 不应为 null");
        assertEquals(userId, extractedUserId, "提取的 UserId 应该与生成的 UserId 相同");
    }

    @Test
    void testGetPhoneFromToken() {
        // 给定
        Long userId = 1L;
        String phone = "13800138000";
        String token = jwtUtil.generateToken(userId, phone);

        // 当
        String extractedPhone = jwtUtil.getPhone(token);

        // 则
        assertNotNull(extractedPhone, "从 Token 中提取的 Phone 不应为 null");
        assertEquals(phone, extractedPhone, "提取的 Phone 应该与生成的 Phone 相同");
    }

    @Test
    void testIsTokenExpired() {
        // 给定
        Long userId = 1L;
        String phone = "13800138000";
        String token = jwtUtil.generateToken(userId, phone);

        // 当
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // 则
        assertFalse(isExpired, "新生成的 Token 不应该过期");
    }

    @Test
    void testValidateTokenWithInvalidToken() {
        // 给定
        String invalidToken = "invalid.token.here";

        // 当 & 则
        assertThrows(Exception.class, () -> {
            jwtUtil.getUserId(invalidToken);
        }, "使用无效 Token 应该抛出异常");
    }
}
