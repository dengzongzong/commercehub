package com.example.commerce.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms.aliyun")
public class AliyunSmsProperties {

    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    /** 短信服务 endpoint */
    private String endpoint = "dysmsapi.aliyuncs.com";
}
