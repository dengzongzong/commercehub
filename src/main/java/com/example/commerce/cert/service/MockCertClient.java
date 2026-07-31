package com.example.commerce.cert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付宝实名认证 Mock 实现
 *
 * 行为对齐真实 AlipayCertClient 三步：
 * 1. initialize() 返回一个模拟的 certify_id
 * 2. generateCertifyUrl() 返回一个本地可访问的认证页面 URL（HTML form）
 * 3. query() 返回认证结果（默认通过）
 *
 * 失败场景：发起认证时 realName 传「测试失败」，后续 query() 返回 false，
 * 便于联调 FAILED 分支。结果在 initialize() 时记忆，与真实环境一致：
 * 同一个 certify_id 多次查结果不变。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockCertClient implements CertClient {

    /** certifyId -> 是否通过，模拟支付宝服务端的状态记忆 */
    private final ConcurrentHashMap<String, Boolean> resultStore = new ConcurrentHashMap<>();

    @Override
    public String initialize(String outerOrderNo, String bizCode, String realName, String certNo) {
        String certifyId = "mock_cert_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        // 姓名为「测试失败」时模拟认证失败
        boolean passed = !"测试失败".equals(realName);
        resultStore.put(certifyId, passed);
        log.info("[MOCK-CERT] 初始化认证 outerOrderNo={} bizCode={} realName={} certNo={} -> certifyId={} passed={}",
                outerOrderNo, bizCode, realName, certNo, certifyId, passed);
        return certifyId;
    }

    @Override
    public String generateCertifyUrl(String certifyId) {
        // 模拟支付宝 pageExecute 返回的 HTML，与真实实现保持 form 结构
        String url = "<form name='alipaycertify' action='https://openapi.alipaydev.com/gateway.do' method='POST'>"
                + "<input type='hidden' name='certify_id' value='" + certifyId + "'/>"
                + "<input type='submit' value='开始认证' style='display:none'/>"
                + "</form>"
                + "<script>document.forms[0].submit();</script>";
        log.info("[MOCK-CERT] 生成认证URL certifyId={}", certifyId);
        return url;
    }

    @Override
    public boolean query(String certifyId) {
        Boolean passed = resultStore.getOrDefault(certifyId, Boolean.TRUE);
        log.info("[MOCK-CERT] 查询认证结果 certifyId={} -> passed={}", certifyId, passed);
        return passed;
    }
}
