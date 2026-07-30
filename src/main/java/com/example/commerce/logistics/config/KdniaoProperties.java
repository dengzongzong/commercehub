package com.example.commerce.logistics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "logistics.kdniao")
public class KdniaoProperties {

    /** 商户ID */
    private String ebusinessId;
    /** API密钥 */
    private String apiKey;
    /** 沙箱地址 */
    private String sandboxUrl = "https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx";
    /** 正式地址 */
    private String prodUrl = "https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx";
}
