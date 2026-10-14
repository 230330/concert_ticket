package com.concert.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 生产环境安全配置校验器
 * 当 spring.profiles.active 含 prod 时，在容器启动阶段校验不安全的默认配置，
 * 任一项不通过即抛出 IllegalStateException 中断启动，避免以开发默认值对外提供服务。
 * 注意：本校验依赖显式声明 prod profile，生产部署必须设置 SPRING_PROFILES_ACTIVE=prod
 * @author: hzf
 * @date: 2026-10-14
 */
@Component
public class ProductionSafetyValidator {

    private static final Logger logger = LoggerFactory.getLogger(ProductionSafetyValidator.class);

    /** 生产环境 profile 名称 */
    private static final String PROD_PROFILE = "prod";

    /** 开发环境默认 JWT 密钥，生产环境出现即视为配置错误 */
    private static final String DEV_JWT_SECRET =
            "ConcertTicketSecretKey2024!@#$%^&*()_+ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** 开发环境默认数据库密码，生产环境出现即视为配置错误 */
    private static final String DEV_DB_PASSWORD = "123456";

    /** JWT 密钥最小长度，HS256 要求密钥不短于 256 位 */
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    /** 开发环境地址标识 */
    private static final String LOCALHOST = "localhost";

    /** 跨域通配符 */
    private static final String WILDCARD_ORIGIN = "*";

    @Resource
    private Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${concert.order.mock-pay-enabled:false}")
    private boolean mockPayEnabled;

    @Value("${alipay.app-id:}")
    private String alipayAppId;

    @Value("${alipay.private-key:}")
    private String alipayPrivateKey;

    @Value("${alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url:}")
    private String alipayNotifyUrl;

    @Value("${alipay.return-url:}")
    private String alipayReturnUrl;

    /**
     * 启动期校验
     * 非 prod 环境直接跳过；prod 环境一次性报出全部违规项后中断启动
     */
    @PostConstruct
    public void validate() {
        if (!environment.acceptsProfiles(Profiles.of(PROD_PROFILE))) {
            return;
        }

        List<String> violations = collectViolations();
        if (!violations.isEmpty()) {
            throw new IllegalStateException("生产环境安全配置校验未通过，已中断启动：\n  - "
                    + String.join("\n  - ", violations));
        }

        logger.info("生产环境安全配置校验通过");
    }

    /**
     * 收集所有不安全的配置项，便于一次性修复
     */
    private List<String> collectViolations() {
        List<String> violations = new ArrayList<>();

        if (StringUtils.isBlank(jwtSecret)
                || DEV_JWT_SECRET.equals(jwtSecret)
                || jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            violations.add("jwt.secret 未通过环境变量 JWT_SECRET 注入，或仍为开发默认值、长度不足 "
                    + MIN_JWT_SECRET_LENGTH + " 位");
        }

        if (StringUtils.isBlank(datasourcePassword) || DEV_DB_PASSWORD.equals(datasourcePassword)) {
            violations.add("spring.datasource.password 未通过环境变量 MYSQL_PASSWORD 注入，或仍为开发默认值");
        }

        if (StringUtils.containsIgnoreCase(allowedOrigins, LOCALHOST)
                || StringUtils.contains(allowedOrigins, WILDCARD_ORIGIN)) {
            violations.add("cors.allowed-origins 仍包含开发地址或通配符，需通过环境变量 CORS_ALLOWED_ORIGINS 配置实际域名");
        }

        if (mockPayEnabled) {
            violations.add("concert.order.mock-pay-enabled 必须为 false，模拟支付会绕过真实支付通道");
        }

        if (StringUtils.isAnyBlank(alipayAppId, alipayPrivateKey, alipayPublicKey)) {
            violations.add("支付宝配置不完整，alipay.app-id / alipay.private-key / alipay.public-key 必须全部通过环境变量注入");
        }

        if (StringUtils.containsIgnoreCase(alipayNotifyUrl, LOCALHOST)
                || StringUtils.containsIgnoreCase(alipayReturnUrl, LOCALHOST)) {
            violations.add("支付宝回调地址仍指向 localhost，alipay.notify-url / alipay.return-url 必须配置为外网可访问地址");
        }

        return violations;
    }
}
