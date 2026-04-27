package com.concert.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @description:
 * @author: hzf
 * @date: 2026年04月27日 16:33
 * @version: 1.0
 */
public class TestBCryptTest {

    /**
     * 测试 BCryptPasswordEncoder 的 encode 方法
     */
    @Test
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("123456");
        System.out.println(encoded);
    }

    /**
     * 测试 BCryptPasswordEncoder 的 matches 方法
     */
    @Test
    public void testMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("123456", "$2a$10$Nk2cLPLnF8ok5JXjH5lFkeA5q7kQZQzQ7Wq6qYp/QmY6jQ5iL6LmK");
        System.out.println(matches); // 如果为 false，说明密文与密码不匹配
    }
}
