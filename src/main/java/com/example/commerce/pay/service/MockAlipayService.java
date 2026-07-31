package com.example.commerce.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.commerce.pay.dto.*;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付宝 Mock 实现
 *
 * 用于无真实账号时本地联调，行为对齐真实 AlipayService：
 * - pay() 返回一个模拟的 HTML 表单（与真实 pageExecute 返回格式一致）
 * - verifyNotify() 永远 true（真实环境会做 RSA2 验签）
 * - parseNotify() 解析 form 表单参数，与真实实现一致
 * - queryTrade() 直接读本地订单状态返回，模拟 TRADE_SUCCESS
 * - refund() 模拟成功
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockAlipayService implements PayService {

    private static final String CHANNEL = "ALIPAY";

    private final PayOrderMapper payOrderMapper;

    public MockAlipayService(PayOrderMapper payOrderMapper) {
        this.payOrderMapper = payOrderMapper;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public PayResp pay(PayReq req) {
        log.info("[MOCK-ALIPAY] 统一下单 outTradeNo={} amount={} subject={}",
                req.getOutTradeNo(), req.getAmount(), req.getSubject());

        // 模拟支付宝手机网站支付返回的 form 表单
        String tradeNo = "2026" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
        String form = "<form name='alipaysubmit' action='https://openapi.alipaydev.com/gateway.do' method='POST'>"
                + "<input type='hidden' name='out_trade_no' value='" + req.getOutTradeNo() + "'/>"
                + "<input type='hidden' name='trade_no' value='" + tradeNo + "'/>"
                + "<input type='hidden' name='total_amount' value='" + req.getAmount() + "'/>"
                + "<input type='submit' value='立即支付' style='display:none'/>"
                + "</form>"
                + "<script>document.forms[0].submit();</script>";

        PayResp resp = PayResp.of(form);
        resp.setTradeNo(tradeNo);
        return resp;
    }

    @Override
    public boolean verifyNotify(String body, Map<String, String> headers) {
        // Mock 模式不做真实验签，直接放行
        log.info("[MOCK-ALIPAY] 回调验签(直接放行) body={}", body);
        return true;
    }

    @Override
    public PayResult parseNotify(String body, Map<String, String> headers) {
        // 与真实实现保持一致：解析 form 表单
        Map<String, String> params = parseFormBody(body);
        PayResult r = new PayResult();
        r.setOutTradeNo(params.get("out_trade_no"));
        r.setTradeNo(params.get("trade_no"));
        String tradeStatus = params.get("trade_status");
        r.setStatus("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus) ? "PAID" : "CLOSED");
        r.setRawBody("success");
        return r;
    }

    @Override
    public RefundResp refund(RefundReq req) {
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        BigDecimal refundAmount = req.getRefundAmount() != null ? req.getRefundAmount() : order.getAmount();
        String refundId = "2026" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("[MOCK-ALIPAY] 退款成功 outTradeNo={} refundNo={} amount={} refundId={}",
                req.getOutTradeNo(), req.getRefundNo(), refundAmount, refundId);
        return RefundResp.ok(refundId);
    }

    @Override
    public PayResult queryTrade(String outTradeNo) {
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", outTradeNo));
        PayResult r = new PayResult();
        r.setOutTradeNo(outTradeNo);
        if (order != null) {
            r.setTradeNo(order.getTradeNo() != null ? order.getTradeNo() : "mock_trade_" + outTradeNo);
            // Mock：本地状态映射成支付宝 trade_status 对应的 PAID/CLOSED
            r.setStatus("PAID".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus()) ? "PAID" : "CLOSED");
        } else {
            r.setStatus("CLOSED");
        }
        log.info("[MOCK-ALIPAY] 主动查单 outTradeNo={} -> {}", outTradeNo, r.getStatus());
        return r;
    }

    private Map<String, String> parseFormBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(java.net.URLDecoder.decode(kv[0], "UTF-8"),
                            java.net.URLDecoder.decode(kv[1], "UTF-8"));
                } catch (Exception ignore) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }
}
