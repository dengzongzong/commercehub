package com.example.commerce.cert.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayUserCertifyOpenCertifyRequest;
import com.alipay.api.request.AlipayUserCertifyOpenInitializeRequest;
import com.alipay.api.request.AlipayUserCertifyOpenQueryRequest;
import com.example.commerce.cert.config.CertProperties;
import com.example.commerce.cert.dto.CertResult;
import com.example.commerce.common.BizException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝实名认证 SDK 封装
 *
 * 三步：
 * 1. alipay.user.certify.open.initialize  初始化，拿到 certify_id
 * 2. alipay.user.certify.open.certify     生成认证链接
 * 3. alipay.user.certify.open.query       查询认证结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayCertClient {

    private final CertProperties props;
    private AlipayClient client;

    @PostConstruct
    public void init() {
        client = new DefaultAlipayClient(
                props.getGateway(),
                props.getAppId(),
                props.getPrivateKey(),
                "json",
                "UTF-8",
                props.getPublicKey(),
                props.getSignType());
    }

    /** 初始化认证，返回 certify_id */
    public String initialize(String outerOrderNo, String bizCode, String realName, String certNo) {
        try {
            AlipayUserCertifyOpenInitializeRequest request = new AlipayUserCertifyOpenInitializeRequest();

            JSONObject identityParam = new JSONObject();
            identityParam.put("cert_name", realName);
            identityParam.put("cert_no", certNo);

            JSONObject merchantConfig = new JSONObject();
            if (props.getReturnUrl() != null) {
                merchantConfig.put("return_url", props.getReturnUrl());
            }

            JSONObject biz = new JSONObject();
            biz.put("outer_order_no", outerOrderNo);
            biz.put("biz_code", bizCode);
            biz.put("identity_param", identityParam);
            biz.put("merchant_config", merchantConfig.toJSONString());

            request.setBizContent(biz.toJSONString());

            return client.execute(request).getCertifyId();
        } catch (Exception e) {
            log.error("支付宝实名认证初始化失败 outerOrderNo={}", outerOrderNo, e);
            throw new BizException("实名认证初始化失败: " + e.getMessage());
        }
    }

    /** 生成认证页面URL */
    public String generateCertifyUrl(String certifyId) {
        try {
            AlipayUserCertifyOpenCertifyRequest request = new AlipayUserCertifyOpenCertifyRequest();
            JSONObject biz = new JSONObject();
            biz.put("certify_id", certifyId);
            request.setBizContent(biz.toJSONString());
            return client.pageExecute(request).getBody();
        } catch (Exception e) {
            log.error("生成认证URL失败 certifyId={}", certifyId, e);
            throw new BizException("生成认证URL失败: " + e.getMessage());
        }
    }

    /** 查询认证结果，返回 true=通过 */
    public boolean query(String certifyId) {
        try {
            AlipayUserCertifyOpenQueryRequest request = new AlipayUserCertifyOpenQueryRequest();
            JSONObject biz = new JSONObject();
            biz.put("certify_id", certifyId);
            request.setBizContent(biz.toJSONString());

            // SDK 返回 String "T"/"F"，转成 boolean
            String passed = client.execute(request).getPassed();
            return "T".equalsIgnoreCase(passed) || "true".equalsIgnoreCase(passed);
        } catch (Exception e) {
            log.error("查询认证结果失败 certifyId={}", certifyId, e);
            throw new BizException("查询认证结果失败: " + e.getMessage());
        }
    }
}
