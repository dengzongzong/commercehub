package com.example.commerce.cert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 实名认证配置，复用支付宝应用(appId/密钥)。
 * 实名认证与支付通常共用同一个支付宝应用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cert.alipay")
public class CertProperties {

    private String appId;
    private String privateKey;
    private String publicKey;
    private String gateway = "https://openapi.alipay.com/gateway.do";
    private String signType = "RSA2";
    /** 认证完成后回跳地址 */
    private String returnUrl;
}
