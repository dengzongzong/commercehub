package com.example.commerce.pay.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.example.commerce.common.BizException;
import com.example.commerce.pay.config.AlipayProperties;
import com.example.commerce.pay.dto.*;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝实现 (手机网站支付 wap)
 */
@Slf4j
@Service
public class AlipayService implements PayService {

    private static final String CHANNEL = "ALIPAY";

    private final AlipayProperties props;
    private final AlipayClient client;
    private final PayOrderMapper payOrderMapper;

    public AlipayService(AlipayProperties props, PayOrderMapper payOrderMapper) {
        this.props = props;
        this.payOrderMapper = payOrderMapper;
        this.client = new DefaultAlipayClient(
                props.getGateway(),
                props.getAppId(),
                props.getPrivateKey(),
                "json",
                props.getCharset(),
                props.getPublicKey(),
                props.getSignType());
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public PayResp pay(PayReq req) {
        try {
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setNotifyUrl(props.getNotifyUrl());
            request.setReturnUrl(props.getReturnUrl());

            Map<String, Object> biz = new HashMap<>();
            biz.put("out_trade_no", req.getOutTradeNo());
            biz.put("total_amount", req.getAmount().toPlainString());
            biz.put("subject", req.getSubject());
            biz.put("product_code", "QUICK_WAP_WAY");
            request.setBizContent(com.alibaba.fastjson.JSON.toJSONString(biz));

            String form = client.pageExecute(request).getBody();
            return PayResp.of(form);
        } catch (Exception e) {
            log.error("支付宝下单失败 outTradeNo={}", req.getOutTradeNo(), e);
            throw new BizException("支付宝下单失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyNotify(String body, Map<String, String> headers) {
        // form 表单参数
        Map<String, String> params = parseFormBody(body);
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    props.getPublicKey(),
                    props.getCharset(),
                    props.getSignType());
        } catch (Exception e) {
            log.error("支付宝验签失败", e);
            return false;
        }
    }

    @Override
    public PayResult parseNotify(String body, Map<String, String> headers) {
        Map<String, String> params = parseFormBody(body);
        PayResult r = new PayResult();
        r.setOutTradeNo(params.get("out_trade_no"));
        r.setTradeNo(params.get("trade_no"));
        // trade_status: TRADE_SUCCESS / TRADE_FINISHED 视为已支付
        String tradeStatus = params.get("trade_status");
        r.setStatus("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus) ? "PAID" : "CLOSED");
        r.setRawBody("success");
        return r;
    }

    @Override
    public RefundResp refund(RefundReq req) {
        try {
            PayOrder order = payOrderMapper.selectOne(
                    new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
            if (order == null) {
                return RefundResp.fail("订单不存在");
            }
            BigDecimal refundAmount = req.getRefundAmount() != null ? req.getRefundAmount() : order.getAmount();

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            Map<String, Object> biz = new HashMap<>();
            biz.put("out_trade_no", req.getOutTradeNo());
            biz.put("refund_amount", refundAmount.toPlainString());
            biz.put("out_request_no", req.getRefundNo());
            if (req.getReason() != null) {
                biz.put("refund_reason", req.getReason());
            }
            request.setBizContent(com.alibaba.fastjson.JSON.toJSONString(biz));

            String resp = client.execute(request).getBody();
            return RefundResp.ok(resp);
        } catch (Exception e) {
            log.error("支付宝退款失败", e);
            return RefundResp.fail(e.getMessage());
        }
    }

    @Override
    public PayResult queryTrade(String outTradeNo) {
        try {
            com.alipay.api.request.AlipayTradeQueryRequest request = new com.alipay.api.request.AlipayTradeQueryRequest();
            Map<String, Object> biz = new HashMap<>();
            biz.put("out_trade_no", outTradeNo);
            request.setBizContent(com.alibaba.fastjson.JSON.toJSONString(biz));

            com.alipay.api.response.AlipayTradeQueryResponse resp = client.execute(request);
            PayResult r = new PayResult();
            r.setOutTradeNo(outTradeNo);
            r.setTradeNo(resp.getTradeNo());
            String status = resp.getTradeStatus();
            r.setStatus("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status) ? "PAID" : "CLOSED");
            return r;
        } catch (Exception e) {
            log.error("支付宝查单失败", e);
            throw new BizException("支付宝查单失败: " + e.getMessage());
        }
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
                    map.put(java.net.URLDecoder.decode(kv[0], props.getCharset()),
                            java.net.URLDecoder.decode(kv[1], props.getCharset()));
                } catch (Exception ignore) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }
}
