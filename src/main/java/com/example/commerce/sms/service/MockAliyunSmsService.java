package com.example.commerce.sms.service;

import com.example.commerce.sms.dto.SmsReq;
import com.example.commerce.sms.dto.SmsResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 阿里云短信 Mock 实现
 *
 * 行为对齐真实 AliyunSmsService：
 * - send() 模拟阿里云返回 Code=OK + BizId，业务层据此落库 SUCCESS
 * - 手机号、模板参数原样打日志，便于联调核对
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockAliyunSmsService implements SmsService {

    private static final String VENDOR = "ALIYUN";

    @Override
    public String vendor() {
        return VENDOR;
    }

    @Override
    public SmsResp send(SmsReq req) {
        // 模拟阿里云回执 BizId，真实返回示例：8002257103236277
        String bizId = String.valueOf(System.currentTimeMillis()) + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("[MOCK-SMS] 发送短信 phone={} templateCode={} params={} -> bizId={}",
                req.getPhone(), req.getTemplateCode(), req.getParams(), bizId);

        // 失败场景可在测试里通过 phone = 13800000000 模拟
        if ("13800000000".equals(req.getPhone())) {
            log.warn("[MOCK-SMS] 命中失败模拟分支 phone={}", req.getPhone());
            return SmsResp.fail("MOCK: isv.MOBILE_NUMBER_ILLEGAL");
        }
        return SmsResp.ok(bizId);
    }
}
