package com.concert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝支付配置属性
 *
 * @author hzf
 * @date 2026/05/28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 支付宝应用ID */
    private String appId;

    /** 应用私钥 */
    private String privateKey;

    /** 支付宝公钥 */
    private String publicKey;

    /** 网关地址 */
    private String gateway = "https://openapi.alipaydev.com/gateway.do";

    /** 签名算法 */
    private String signType = "RSA2";

    /** 字符集 */
    private String charset = "UTF-8";

    /** 数据格式 */
    private String format = "json";

    /** 异步通知地址 */
    private String notifyUrl;

    /** 同步跳转地址 */
    private String returnUrl;
}
