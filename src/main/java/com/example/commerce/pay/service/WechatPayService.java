package com.example.commerce.pay.service;

import com.example.commerce.common.BizException;
import com.example.commerce.pay.config.WechatProperties;
import com.example.commerce.pay.dto.*;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付实现 (JSAPI) - 适配 wechatpay-java 0.2.17
 */
@Slf4j
@Service
public class WechatPayService implements PayService {

    private static final String CHANNEL = "WECHAT";

    private final WechatProperties props;
    private final PayOrderMapper payOrderMapper;
    private final Config config;
    private final JsapiService jsapiService;
    private final JsapiServiceExtension queryService;
    private final RefundService refundService;
    private final NotificationConfig notificationConfig;

    public WechatPayService(WechatProperties props, PayOrderMapper payOrderMapper) {
        this.props = props;
        this.payOrderMapper = payOrderMapper;

        RSAAutoCertificateConfig.Builder builder = new RSAAutoCertificateConfig.Builder()
                .merchantId(props.getMchId())
                .merchantSerialNumber(props.getCertSerialNo())
                .apiV3Key(props.getApiV3Key());

        // 优先用私钥文件路径，其次用私钥内容
        if (props.getPrivateKeyPath() != null && !props.getPrivateKeyPath().isEmpty()) {
            builder.privateKeyFromPath(props.getPrivateKeyPath());
        } else if (props.getPrivateKey() != null && !props.getPrivateKey().isEmpty()) {
            builder.privateKey(props.getPrivateKey());
        } else {
            throw new IllegalStateException("微信支付私钥未配置，请设置 wechat.private-key-path 或 wechat.private-key");
        }

        this.config = builder.build();
        this.notificationConfig = (NotificationConfig) this.config;
        this.jsapiService = new JsapiService.Builder().config(config).build();
        this.queryService = new JsapiServiceExtension.Builder().config(config).build();
        this.refundService = new RefundService.Builder().config(config).build();
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public PayResp pay(PayReq req) {
        try {
            PrepayRequest request = new PrepayRequest();
            request.setAppid(props.getAppId());
            request.setMchid(props.getMchId());
            request.setDescription(req.getSubject());
            request.setOutTradeNo(req.getOutTradeNo());
            request.setNotifyUrl(props.getNotifyUrl());

            Amount amount = new Amount();
            amount.setTotal(req.getAmount().multiply(new BigDecimal("100")).intValue());
            amount.setCurrency("CNY");
            request.setAmount(amount);

            Payer payer = new Payer();
            payer.setOpenid(req.getOpenid());
            request.setPayer(payer);

            PrepayResponse resp = jsapiService.prepay(request);
            return PayResp.of(resp.getPrepayId());
        } catch (Exception e) {
            log.error("微信下单失败 outTradeNo={}", req.getOutTradeNo(), e);
            throw new BizException("微信下单失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyNotify(String body, Map<String, String> headers) {
        // SDK 在 parse 时自动验签
        return true;
    }

    @Override
    public PayResult parseNotify(String body, Map<String, String> headers) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(getHeader(headers, "Wechatpay-Serial"))
                    .nonce(getHeader(headers, "Wechatpay-Nonce"))
                    .signature(getHeader(headers, "Wechatpay-Signature"))
                    .timestamp(getHeader(headers, "Wechatpay-Timestamp"))
                    .body(body)
                    .build();

            NotificationParser parser = new NotificationParser(notificationConfig);
            Transaction tx = parser.parse(requestParam, Transaction.class);

            PayResult r = new PayResult();
            r.setOutTradeNo(tx.getOutTradeNo());
            r.setTradeNo(tx.getTransactionId());
            r.setStatus(tx.getTradeState() == Transaction.TradeStateEnum.SUCCESS ? "PAID" : "CLOSED");
            r.setRawBody("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
            return r;
        } catch (Exception e) {
            log.error("微信回调解析失败", e);
            throw new BizException("微信回调解析失败: " + e.getMessage());
        }
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

            CreateRequest request = new CreateRequest();
            request.setOutTradeNo(req.getOutTradeNo());
            request.setOutRefundNo(req.getRefundNo());
            request.setReason(req.getReason());
            request.setNotifyUrl(props.getNotifyUrl());

            AmountReq amount = new AmountReq();
            amount.setRefund(refundAmount.multiply(new BigDecimal("100")).longValue());
            amount.setTotal(order.getAmount().multiply(new BigDecimal("100")).longValue());
            amount.setCurrency("CNY");
            request.setAmount(amount);

            Refund refund = refundService.create(request);
            return RefundResp.ok(refund.getRefundId());
        } catch (Exception e) {
            log.error("微信退款失败", e);
            return RefundResp.fail(e.getMessage());
        }
    }

    @Override
    public PayResult queryTrade(String outTradeNo) {
        try {
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(props.getMchId());
            request.setOutTradeNo(outTradeNo);

            Transaction tx = queryService.queryOrderByOutTradeNo(request);
            PayResult r = new PayResult();
            r.setOutTradeNo(outTradeNo);
            r.setTradeNo(tx.getTransactionId());
            r.setStatus(tx.getTradeState() == Transaction.TradeStateEnum.SUCCESS ? "PAID" : "CLOSED");
            return r;
        } catch (Exception e) {
            log.error("微信查单失败", e);
            throw new BizException("微信查单失败: " + e.getMessage());
        }
    }

    private String getHeader(Map<String, String> headers, String name) {
        if (headers == null) return null;
        String v = headers.get(name);
        if (v == null) v = headers.get(name.toLowerCase());
        return v;
    }
}
