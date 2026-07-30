package com.example.commerce.sms.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.example.commerce.common.BizException;
import com.example.commerce.sms.config.AliyunSmsProperties;
import com.example.commerce.sms.dto.SmsReq;
import com.example.commerce.sms.dto.SmsResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信实现
 */
@Slf4j
@Service
public class AliyunSmsService implements SmsService {

    private static final String VENDOR = "ALIYUN";

    private final AliyunSmsProperties props;
    private final Client client;

    public AliyunSmsService(AliyunSmsProperties props) {
        this.props = props;
        try {
            Config config = new Config()
                    .setAccessKeyId(props.getAccessKeyId())
                    .setAccessKeySecret(props.getAccessKeySecret())
                    .setEndpoint(props.getEndpoint());
            this.client = new Client(config);
        } catch (Exception e) {
            throw new IllegalStateException("初始化阿里云短信客户端失败", e);
        }
    }

    @Override
    public String vendor() {
        return VENDOR;
    }

    @Override
    public SmsResp send(SmsReq req) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(req.getPhone())
                    .setSignName(props.getSignName())
                    .setTemplateCode(req.getTemplateCode())
                    .setTemplateParam(req.getParams());

            SendSmsResponse resp = client.sendSms(request);
            String code = resp.getBody().getCode();
            if ("OK".equals(code)) {
                return SmsResp.ok(resp.getBody().getBizId());
            }
            log.warn("短信发送失败 phone={} code={} msg={}",
                    req.getPhone(), code, resp.getBody().getMessage());
            return SmsResp.fail(resp.getBody().getMessage());
        } catch (Exception e) {
            log.error("阿里云短信发送异常 phone={}", req.getPhone(), e);
            throw new BizException("短信发送失败: " + e.getMessage());
        }
    }
}
