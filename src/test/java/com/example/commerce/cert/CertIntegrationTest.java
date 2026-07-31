package com.example.commerce.cert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.commerce.AbstractMockIntegrationTest;
import com.example.commerce.cert.dto.CertInitializeReq;
import com.example.commerce.cert.dto.CertResult;
import com.example.commerce.cert.entity.CertRecord;
import com.example.commerce.cert.mapper.CertRecordMapper;
import com.example.commerce.common.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实名认证集成测试（Mock 模式）
 *
 * 覆盖：发起认证 -> 查询结果（PASSED / FAILED 两个分支）
 */
@DisplayName("实名认证模块-集成测试")
class CertIntegrationTest extends AbstractMockIntegrationTest {

    @Autowired
    private CertRecordMapper certRecordMapper;

    @Test
    @DisplayName("发起认证 -> 查询结果为 PASSED")
    void cert_passed() {
        CertInitializeReq req = new CertInitializeReq();
        req.setUserId("TEST_USER_" + System.currentTimeMillis());
        req.setRealName("张三");
        req.setCertNo("110101199001011234");
        req.setBizCode("FACE");

        // ====== 1. 发起认证 ======
        Response<CertResult> resp = postJson("/cert/initialize", req,
                new ParameterizedTypeReference<Response<CertResult>>() {}).getBody();
        assertNotNull(resp);
        assertEquals(0, resp.getCode(), "发起认证应返回成功");
        assertNotNull(resp.getData());
        assertNotNull(resp.getData().getBizNo(), "应返回业务流水号");
        assertNotNull(resp.getData().getCertifyId(), "应返回 certifyId");
        assertNotNull(resp.getData().getCertifyUrl(), "应返回认证URL");
        assertEquals("PROCESSING", resp.getData().getStatus(), "初始状态应为 PROCESSING");

        // 落库校验
        CertRecord record = certRecordMapper.selectOne(
                new QueryWrapper<CertRecord>().eq("biz_no", resp.getData().getBizNo()));
        assertNotNull(record, "认证记录应落库");
        assertEquals("PROCESSING", record.getStatus(), "初始状态应为 PROCESSING");

        // ====== 2. 查询认证结果 ======
        Response<CertResult> queryResp = getJson("/cert/query/" + resp.getData().getBizNo(),
                new ParameterizedTypeReference<Response<CertResult>>() {}).getBody();
        assertNotNull(queryResp);
        assertEquals(0, queryResp.getCode());
        assertEquals("PASSED", queryResp.getData().getStatus(), "Mock 默认认证通过");

        // 状态机校验：PROCESSING -> PASSED
        CertRecord passed = certRecordMapper.selectOne(
                new QueryWrapper<CertRecord>().eq("biz_no", resp.getData().getBizNo()));
        assertEquals("PASSED", passed.getStatus(), "本地状态应同步为 PASSED");

        // ====== 3. 幂等校验：再次查询不应重复更新 ======
        getJson("/cert/query/" + resp.getData().getBizNo(),
                new ParameterizedTypeReference<Response<CertResult>>() {});
        assertEquals("PASSED", certRecordMapper.selectOne(
                new QueryWrapper<CertRecord>().eq("biz_no", resp.getData().getBizNo())).getStatus());

        // ====== 4. 按用户ID查最近认证记录 ======
        Response<CertRecord> recordResp = getJson("/cert/record/" + req.getUserId(),
                new ParameterizedTypeReference<Response<CertRecord>>() {}).getBody();
        assertNotNull(recordResp);
        assertEquals(0, recordResp.getCode());
    }

    @Test
    @DisplayName("发起认证 -> 查询结果为 FAILED（命中 Mock 失败分支）")
    void cert_failed() {
        CertInitializeReq req = new CertInitializeReq();
        req.setUserId("TEST_USER_FAIL_" + System.currentTimeMillis());
        // Mock 约定：realName=「测试失败」时，query() 返回 false
        req.setRealName("测试失败");
        req.setCertNo("110101199001011235");
        req.setBizCode("FACE");

        Response<CertResult> resp = postJson("/cert/initialize", req,
                new ParameterizedTypeReference<Response<CertResult>>() {}).getBody();
        assertNotNull(resp);
        assertEquals(0, resp.getCode());

        // 查询应得到 FAILED
        Response<CertResult> queryResp = getJson("/cert/query/" + resp.getData().getBizNo(),
                new ParameterizedTypeReference<Response<CertResult>>() {}).getBody();
        assertNotNull(queryResp);
        assertEquals(0, queryResp.getCode());
        assertEquals("FAILED", queryResp.getData().getStatus(), "失败分支应返回 FAILED");

        // 状态机校验
        CertRecord failed = certRecordMapper.selectOne(
                new QueryWrapper<CertRecord>().eq("biz_no", resp.getData().getBizNo()));
        assertEquals("FAILED", failed.getStatus(), "本地状态应同步为 FAILED");
    }
}
