package com.example.commerce.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.commerce.pay.dto.*;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayOrderMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 微信支付 Mock 实现
 *
 * 行为对齐真实 WechatPayService：
 * - pay() 返回模拟的 prepay_id / wxJsApiParam
 * - verifyNotify() 永远 true（真实环境会基于平台证书验签）
 * - parseNotify() 解析 JSON 报文，与微信 v3 回调结构一致
 * - queryTrade() 读本地订单状态，模拟 SUCCESS
 * - refund() 模拟成功
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockWechatPayService implements PayService {

    private static final String CHANNEL = "WECHAT";

    private final PayOrderMapper payOrderMapper;

    public MockWechatPayService(PayOrderMapper payOrderMapper) {
        this.payOrderMapper = payOrderMapper;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public PayResp pay(PayReq req) {
        log.info("[MOCK-WECHAT] 统一下单 outTradeNo={} amount={} openid={}",
                req.getOutTradeNo(), req.getAmount(), req.getOpenid());

        // 模拟微信 JSAPI 下单返回，前端调起支付用的参数
        String prepayId = "wx" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
        JSONObject jsApi = new JSONObject();
        jsApi.put("appId", "wxMOCKAPPID");
        jsApi.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        jsApi.put("nonceStr", java.util.UUID.randomUUID().toString().replace("-", ""));
        jsApi.put("package", "prepay_id=" + prepayId);
        jsApi.put("signType", "RSA");
        jsApi.put("paySign", "MOCK_SIGN_" + prepayId);

        PayResp resp = PayResp.of(jsApi.toJSONString());
        resp.setTradeNo(prepayId);
        return resp;
    }

    @Override
    public boolean verifyNotify(String body, Map<String, String> headers) {
        log.info("[MOCK-WECHAT] 回调验签(直接放行) body={}", body);
        return true;
    }

    @Override
    public PayResult parseNotify(String body, Map<String, String> headers) {
        // 与微信 v3 回调结构对齐：resource.ciphertext 解密后的 JSON
        PayResult r = new PayResult();
        try {
            // 测试代码可以直接传明文 JSON：{"out_trade_no":"..","trade_no":"..","trade_state":"SUCCESS"}
            JSONObject json = JSON.parseObject(body);
            JSONObject plain = json.containsKey("resource")
                    ? JSON.parseObject(json.getJSONObject("resource").getString("ciphertext"))
                    : json;
            r.setOutTradeNo(plain.getString("out_trade_no"));
            r.setTradeNo(plain.getString("transaction_id") != null
                    ? plain.getString("transaction_id")
                    : plain.getString("trade_no"));
            String state = plain.getString("trade_state");
            r.setStatus("SUCCESS".equals(state) ? "PAID" : "CLOSED");
            // 微信回调应答
            JSONObject ack = new JSONObject();
            ack.put("code", "SUCCESS");
            ack.put("message", "成功");
            r.setRawBody(ack.toJSONString());
        } catch (Exception e) {
            log.warn("[MOCK-WECHAT] 回调报文解析失败 body={}", body, e);
            r.setRawBody("{\"code\":\"FAIL\",\"message\":\"解析失败\"}");
        }
        return r;
    }

    @Override
    public RefundResp refund(RefundReq req) {
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        String refundId = "wx_refund_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("[MOCK-WECHAT] 退款成功 outTradeNo={} refundNo={} amount={} refundId={}",
                req.getOutTradeNo(), req.getRefundNo(),
                req.getRefundAmount() != null ? req.getRefundAmount() : (order != null ? order.getAmount() : null),
                refundId);
        return RefundResp.ok(refundId);
    }

    @Override
    public PayResult queryTrade(String outTradeNo) {
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", outTradeNo));
        PayResult r = new PayResult();
        r.setOutTradeNo(outTradeNo);
        if (order != null) {
            r.setTradeNo(order.getTradeNo() != null ? order.getTradeNo() : "wx_mock_" + outTradeNo);
            r.setStatus("PAID".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus()) ? "PAID" : "CLOSED");
        } else {
            r.setStatus("CLOSED");
        }
        log.info("[MOCK-WECHAT] 主动查单 outTradeNo={} -> {}", outTradeNo, r.getStatus());
        return r;
    }
}
