package com.example.commerce.pay;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.commerce.AbstractMockIntegrationTest;
import com.example.commerce.common.Response;
import com.example.commerce.pay.dto.PayReq;
import com.example.commerce.pay.dto.PayResp;
import com.example.commerce.pay.dto.PayResult;
import com.example.commerce.pay.dto.RefundReq;
import com.example.commerce.pay.dto.RefundResp;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayOrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付全流程集成测试（Mock 模式）
 *
 * 覆盖业务链路：下单 -> 模拟第三方异步回调 -> 主动查单 -> 退款
 * 这条链路与真实环境的差异仅在「第三方调用」一层（由 Mock 实现），
 * 业务编排、状态机、幂等逻辑完全相同。
 */
@DisplayName("支付模块-集成测试")
class PayIntegrationTest extends AbstractMockIntegrationTest {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Test
    @DisplayName("支付宝：下单 -> 回调 -> 查单 -> 退款，全链路")
    void alipay_fullFlow() {
        // ====== 1. 发起支付 ======
        PayReq req = new PayReq();
        req.setOutTradeNo("TEST_ALI_" + System.currentTimeMillis());
        req.setChannel("ALIPAY");
        req.setSubject("测试订单-支付宝");
        req.setAmount(new BigDecimal("9.99"));

        Response<PayResp> payResp = postJson("/pay/pay", req,
                new ParameterizedTypeReference<Response<PayResp>>() {}).getBody();

        assertNotNull(payResp);
        assertEquals(0, payResp.getCode(), "下单应返回成功");
        // Mock 返回的 payBody 是一段 HTML 表单，与真实 pageExecute 一致
        assertNotNull(payResp.getData(), "payBody 不应为空");

        // 落库校验：状态应为 PAYING
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        assertNotNull(order, "下单后应落库");
        assertEquals("PAYING", order.getStatus(), "初始状态应为 PAYING");

        // ====== 2. 模拟支付宝异步回调 ======
        // 报文格式与真实支付宝回调一致：form 表单，trade_status=TRADE_SUCCESS
        String tradeNo = "2026" + System.currentTimeMillis();
        String notifyBody = "out_trade_no=" + req.getOutTradeNo()
                + "&trade_no=" + tradeNo
                + "&trade_status=TRADE_SUCCESS"
                + "&total_amount=9.99";

        String ack = postRaw("/pay/notify/alipay", notifyBody, MediaType.APPLICATION_FORM_URLENCODED);
        assertEquals("success", ack, "支付宝回调应答应为 success");

        // 状态机校验：PAYING -> PAID
        PayOrder paid = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        assertEquals("PAID", paid.getStatus(), "回调后状态应为 PAID");
        assertEquals(tradeNo, paid.getTradeNo(), "应保存第三方交易号");

        // ====== 3. 幂等性校验：重复回调不应产生副作用 ======
        postRaw("/pay/notify/alipay", notifyBody, MediaType.APPLICATION_FORM_URLENCODED);
        assertEquals("PAID", payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo())).getStatus(),
                "重复回调不应改变订单状态");

        // ====== 4. 主动查单 ======
        Response<PayResult> queryResp = getJson("/pay/query/" + req.getOutTradeNo(),
                new ParameterizedTypeReference<Response<PayResult>>() {}).getBody();
        assertNotNull(queryResp);
        assertEquals(0, queryResp.getCode());

        // ====== 5. 退款 ======
        RefundReq refundReq = new RefundReq();
        refundReq.setOutTradeNo(req.getOutTradeNo());
        refundReq.setRefundNo("RF_" + System.currentTimeMillis());
        refundReq.setReason("测试退款");

        Response<RefundResp> refundResp = postJson("/pay/refund", refundReq,
                new ParameterizedTypeReference<Response<RefundResp>>() {}).getBody();
        assertNotNull(refundResp);
        assertEquals(0, refundResp.getCode(), "退款应返回成功");

        // 退款后状态应为 REFUNDED
        PayOrder refunded = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        assertEquals("REFUNDED", refunded.getStatus(), "退款后状态应为 REFUNDED");
    }

    @Test
    @DisplayName("微信支付：下单 -> 回调 -> 查单")
    void wechat_fullFlow() {
        // ====== 1. 发起支付 ======
        PayReq req = new PayReq();
        req.setOutTradeNo("TEST_WX_" + System.currentTimeMillis());
        req.setChannel("WECHAT");
        req.setSubject("测试订单-微信");
        req.setAmount(new BigDecimal("19.90"));
        req.setOpenid("oMockOpenid123456");

        Response<PayResp> payResp = postJson("/pay/pay", req,
                new ParameterizedTypeReference<Response<PayResp>>() {}).getBody();
        assertNotNull(payResp);
        assertEquals(0, payResp.getCode());
        // Mock 返回的 payBody 是 JSAPI 调起参数 JSON，与真实微信返回结构一致
        assertNotNull(payResp.getData());

        // ====== 2. 模拟微信 v3 异步回调 ======
        // 报文格式与真实微信回调对齐：外层 resource.ciphertext 为加密内容，
        // Mock 解析支持直接传明文 JSON 简化测试
        String transactionId = "wx_tx_" + System.currentTimeMillis();
        String notifyBody = "{\"out_trade_no\":\"" + req.getOutTradeNo() + "\","
                + "\"transaction_id\":\"" + transactionId + "\","
                + "\"trade_state\":\"SUCCESS\"}";

        String ack = postRaw("/pay/notify/wechat", notifyBody, MediaType.APPLICATION_JSON);
        assertNotNull(ack);
        assertTrue(ack.contains("SUCCESS"), "微信回调应答应包含 SUCCESS");

        // 状态机校验
        PayOrder paid = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        assertEquals("PAID", paid.getStatus(), "微信回调后状态应为 PAID");
        assertEquals(transactionId, paid.getTradeNo(), "应保存微信交易号");

        // ====== 3. 主动查单 ======
        Response<PayResult> queryResp = getJson("/pay/query/" + req.getOutTradeNo(),
                new ParameterizedTypeReference<Response<PayResult>>() {}).getBody();
        assertNotNull(queryResp);
        assertEquals(0, queryResp.getCode());
    }
}
