package com.example.commerce.sms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.commerce.AbstractMockIntegrationTest;
import com.example.commerce.common.Response;
import com.example.commerce.sms.dto.SmsReq;
import com.example.commerce.sms.dto.SmsResp;
import com.example.commerce.sms.entity.SmsRecord;
import com.example.commerce.sms.mapper.SmsRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 短信集成测试（Mock 模式）
 *
 * 覆盖：发送成功 + 发送失败两个分支，并校验落库记录
 */
@DisplayName("短信模块-集成测试")
class SmsIntegrationTest extends AbstractMockIntegrationTest {

    @Autowired
    private SmsRecordMapper smsRecordMapper;

    @Test
    @DisplayName("发送短信-成功")
    void send_success() {
        SmsReq req = new SmsReq();
        req.setPhone("13900139000");
        req.setTemplateCode("SMS_TEST_CODE");
        req.setParams("{\"code\":\"8888\"}");

        Response<SmsResp> resp = postJson("/sms/send", req,
                new ParameterizedTypeReference<Response<SmsResp>>() {}).getBody();
        assertNotNull(resp);
        assertEquals(0, resp.getCode(), "接口应返回成功");
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isSuccess(), "Mock 短信应发送成功");
        assertNotNull(resp.getData().getBizId(), "应返回回执 BizId");

        // 落库校验：status=SUCCESS，bizId 不为空
        SmsRecord record = smsRecordMapper.selectOne(
                new QueryWrapper<SmsRecord>()
                        .eq("phone", req.getPhone())
                        .eq("template_code", req.getTemplateCode())
                        .orderByDesc("id")
                        .last("LIMIT 1"));
        assertNotNull(record, "短信发送应落库");
        assertEquals("SUCCESS", record.getStatus(), "Mock 短信应发送成功");
        assertNotNull(record.getBizId(), "应保存回执 BizId");
        assertEquals("ALIYUN", record.getVendor());
    }

    @Test
    @DisplayName("发送短信-失败（命中 Mock 失败分支）")
    void send_fail() {
        // MockAliyunSmsService 约定：phone=13800000000 时返回失败
        SmsReq req = new SmsReq();
        req.setPhone("13800000000");
        req.setTemplateCode("SMS_TEST_CODE");
        req.setParams("{\"code\":\"9999\"}");

        Response<SmsResp> resp = postJson("/sms/send", req,
                new ParameterizedTypeReference<Response<SmsResp>>() {}).getBody();
        assertNotNull(resp);
        // 接口层仍返回 code=0（业务上的失败，不是 HTTP 错误），SmsResp.success=false
        assertEquals(0, resp.getCode(), "业务失败时 Response.code 仍为 0");
        assertNotNull(resp.getData());
        assertFalse(resp.getData().isSuccess(), "失败分支 SmsResp.success 应为 false");

        // 落库校验：status=FAIL
        SmsRecord record = smsRecordMapper.selectOne(
                new QueryWrapper<SmsRecord>()
                        .eq("phone", req.getPhone())
                        .eq("template_code", req.getTemplateCode())
                        .orderByDesc("id")
                        .last("LIMIT 1"));
        assertNotNull(record, "失败短信也应落库");
        assertEquals("FAIL", record.getStatus(), "Mock 失败分支应落库 FAIL");
        assertNotNull(record.getErrorMsg(), "应保存失败原因");
    }
}
