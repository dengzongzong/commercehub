package com.example.commerce.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pay.alipay")
public class AlipayProperties {

    private String appId;
    private String privateKey;
    private String publicKey;
    private String gateway = "https://openapi.alipay.com/gateway.do";
    /** 异步回调地址 */
    private String notifyUrl;
    /** 同步返回页面 */
    private String returnUrl;
    private String signType = "RSA2";
    private String charset = "UTF-8";
}
