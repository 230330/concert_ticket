package com.concert.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ProductionSafetyValidator 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProductionSafetyValidatorTest {

    /** 生产环境期望的 JWT 密钥 */
    private static final String SECURE_JWT_SECRET =
            "ProductionJwtSecretKey2026!@#$%^&*()_ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** 生产环境期望的回调域名 */
    private static final String SECURE_ORIGIN = "https://concert.example.com";

    /** 开发环境默认 JWT 密钥 */
    private static final String DEV_JWT_SECRET =
            "ConcertTicketSecretKey2024!@#$%^&*()_+ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** 开发环境默认数据库密码 */
    private static final String DEV_DB_PASSWORD = "123456";

    @Mock
    private Environment environment;

    private ProductionSafetyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductionSafetyValidator();
        ReflectionTestUtils.setField(validator, "environment", environment);
        injectSecureConfig();
    }

    /**
     * 注入一套完整的安全配置，各用例只覆盖需要破坏的字段
     */
    private void injectSecureConfig() {
        ReflectionTestUtils.setField(validator, "jwtSecret", SECURE_JWT_SECRET);
        ReflectionTestUtils.setField(validator, "datasourcePassword", "ProdDbPassword!2026");
        ReflectionTestUtils.setField(validator, "allowedOrigins", SECURE_ORIGIN);
        ReflectionTestUtils.setField(validator, "mockPayEnabled", false);
        ReflectionTestUtils.setField(validator, "alipayAppId", "2021000000000000");
        ReflectionTestUtils.setField(validator, "alipayPrivateKey", "MIIEvQIBADANBgkqhkiG9w0BAQEFAASC");
        ReflectionTestUtils.setField(validator, "alipayPublicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A");
        ReflectionTestUtils.setField(validator, "alipayNotifyUrl",
                SECURE_ORIGIN + "/api/pay/alipay/notify");
        ReflectionTestUtils.setField(validator, "alipayReturnUrl",
                SECURE_ORIGIN + "/#/order/my");
    }

    @Nested
    @DisplayName("非生产环境")
    class NonProd {

        @Test
        @DisplayName("未激活 prod profile 时跳过校验，开发默认值不阻断启动")
        void testValidate_NonProdProfile_SkipsValidation() {
            // Given
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
            ReflectionTestUtils.setField(validator, "jwtSecret", DEV_JWT_SECRET);
            ReflectionTestUtils.setField(validator, "datasourcePassword", DEV_DB_PASSWORD);
            ReflectionTestUtils.setField(validator, "mockPayEnabled", true);

            // When & Then
            assertDoesNotThrow(() -> validator.validate());
        }
    }

    @Nested
    @DisplayName("生产环境 - 校验通过")
    class ProdPass {

        @Test
        @DisplayName("配置齐备时不抛出异常")
        void testValidate_ProdProfileWithSecureConfig_Passes() {
            // Given
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

            // When & Then
            assertDoesNotThrow(() -> validator.validate());
        }
    }

    @Nested
    @DisplayName("生产环境 - 校验失败")
    class ProdFail {

        @Test
        @DisplayName("开发默认值全部生效时一次性报出所有违规项")
        void testValidate_ProdProfileWithDevDefaults_ThrowsWithAllViolations() {
            // Given
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
            ReflectionTestUtils.setField(validator, "jwtSecret", DEV_JWT_SECRET);
            ReflectionTestUtils.setField(validator, "datasourcePassword", DEV_DB_PASSWORD);
            ReflectionTestUtils.setField(validator, "allowedOrigins", "http://localhost:9528");
            ReflectionTestUtils.setField(validator, "mockPayEnabled", true);
            ReflectionTestUtils.setField(validator, "alipayAppId", "");
            ReflectionTestUtils.setField(validator, "alipayPrivateKey", "");
            ReflectionTestUtils.setField(validator, "alipayPublicKey", "");
            ReflectionTestUtils.setField(validator, "alipayNotifyUrl",
                    "http://localhost:8090/api/pay/alipay/notify");
            ReflectionTestUtils.setField(validator, "alipayReturnUrl",
                    "http://localhost:9528/#/order/my");

            // When
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> validator.validate());

            // Then
            assertTrue(ex.getMessage().contains("jwt.secret"));
            assertTrue(ex.getMessage().contains("spring.datasource.password"));
            assertTrue(ex.getMessage().contains("cors.allowed-origins"));
            assertTrue(ex.getMessage().contains("mock-pay-enabled"));
            assertTrue(ex.getMessage().contains("支付宝配置不完整"));
            assertTrue(ex.getMessage().contains("回调地址仍指向 localhost"));
        }

        @Test
        @DisplayName("模拟支付未关闭时中断启动")
        void testValidate_MockPayEnabled_ThrowsException() {
            // Given
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
            ReflectionTestUtils.setField(validator, "mockPayEnabled", true);

            // When
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> validator.validate());

            // Then
            assertTrue(ex.getMessage().contains("mock-pay-enabled"));
        }

        @Test
        @DisplayName("JWT 密钥长度不足时中断启动")
        void testValidate_ShortJwtSecret_ThrowsException() {
            // Given
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
            ReflectionTestUtils.setField(validator, "jwtSecret", "too-short-secret");

            // When
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> validator.validate());

            // Then
            assertTrue(ex.getMessage().contains("jwt.secret"));
        }

        @Test
        @DisplayName("跨域使用通配符时中断启动")
        void testValidate_WildcardOrigin_ThrowsException() {
            // Given
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
            ReflectionTestUtils.setField(validator, "allowedOrigins", "*");

            // When
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> validator.validate());

            // Then
            assertTrue(ex.getMessage().contains("cors.allowed-origins"));
        }
    }
}
