package com.example.commerce.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pay.wechat")
public class WechatProperties {

    private String appId;
    private String mchId;
    /** APIv3 密钥 */
    private String apiV3Key;
    /** 商户证书序列号 */
    private String certSerialNo;
    /** 商户私钥(apiclient_key.pem 内容)，与 privateKeyPath 二选一 */
    private String privateKey;
    /** 商户私钥文件路径(apiclient_key.pem)，与 privateKey 二选一，推荐用路径 */
    private String privateKeyPath;
    /** 微信平台证书可通过 SDK 自动下载，无需配置 */
    private String notifyUrl;
}
