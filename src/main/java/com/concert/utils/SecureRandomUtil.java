package com.concert.utils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * @description: 安全随机数工具类
 * @author: hzf
 * @date: 2026年04月23日 16:01
 * @version: 1.0
 */
public class SecureRandomUtil {
    private static final SecureRandom SECURE_RANDOM;

    static {
        try {
            // 使用 SHA1PRNG 算法，兼容性好；也可用 NativePRNG
            SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
            // 强制播种（系统熵源）
            SECURE_RANDOM.nextBytes(new byte[16]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SecureRandom", e);
        }
    }

    /**
     * 生成纯数字验证码
     * @param length 位数（如6）
     */
    public static String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10)); // 0-9
        }
        return sb.toString();
    }

    /**
     * 生成字母数字混合取票码（大写字母+数字）
     * @param length 长度（如8）
     */
    public static String generateAlphanumericCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789"; // 排除容易混淆的 O,0,I,1
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = SECURE_RANDOM.nextInt(chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    /**
     * 生成 Base64 URL-Safe 随机字符串（更安全，长度可控）
     */
    public static String generateBase64Token(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
