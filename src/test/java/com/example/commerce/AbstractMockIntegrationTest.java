package com.example.commerce;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

/**
 * Mock 模式集成测试基类
 *
 * 设计要点（解释「为什么这样能确保调完跟真实环境没区别」）：
 *
 * 1. @ActiveProfiles("mock") 加载 application-mock.yml：
 *    - 数据源切到 H2 内存库，schema-h2.sql 自动建表，零外部依赖
 *    - mock.enabled=true 让所有 @ConditionalOnProperty(true) 的 Mock 实现生效，
 *      同时真实实现（AlipayService / WechatPayService / KdniaoLogisticsService /
 *      AliyunSmsService / AlipayCertClient）被 Spring 跳过，不会因缺密钥启动失败
 *
 * 2. 走真实的 Controller -> BizService -> Service 接口 全链路：
 *    - Mock 实现的入参、返回结构、状态机字段都与真实实现一一对应
 *    - 因此「下单 → 落库 → 回调验签/解析 → 状态机更新 → 查单 → 退款」
 *      这条业务链路在 Mock 下跑通后，切到真实环境只需把 mock.enabled 改成 false，
 *      业务代码一行不改，行为一致
 *
 * 3. 真实/Mock 切换的契约由 @ConditionalOnProperty 保证：
 *    - 真实实现 havingValue=false matchIfMissing=true（默认走真实）
 *    - Mock 实现  havingValue=true （仅 mock profile 加载）
 *    两者实现同一接口，对 BizService 透明
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
public abstract class AbstractMockIntegrationTest {

    @LocalServerPort
    protected int port;

    /** application.yml 配置了 server.servlet.context-path=/api，所有接口前缀都是 /api */
    protected String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    /**
     * 用最普通的 RestTemplate 即可，避免 TestRestTemplate 在某些版本下的行为差异。
     * 子类按需注入或直接用 restTemplate。
     */
    protected final RestTemplate restTemplate = new RestTemplate();

    /** JSON 请求头 */
    protected HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /**
     * POST 请求并按指定泛型类型反序列化 Response<T>。
     * 必须传 ParameterizedTypeReference 才能让 Jackson 正确还原 data 字段的类型。
     */
    protected <T> ResponseEntity<T> postJson(String path, Object body, ParameterizedTypeReference<T> typeRef) {
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), typeRef);
    }

    /** GET 请求并按指定泛型类型反序列化 Response<T>。 */
    protected <T> ResponseEntity<T> getJson(String path, ParameterizedTypeReference<T> typeRef) {
        return restTemplate.exchange(url(path), HttpMethod.GET, null, typeRef);
    }

    /** POST 原始字符串（如 form 表单回调），返回字符串应答。 */
    protected String postRaw(String path, String body, MediaType contentType) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(contentType);
        return restTemplate.postForObject(url(path), new HttpEntity<>(body, h), String.class);
    }
}
