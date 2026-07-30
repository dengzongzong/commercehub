package com.example.commerce.pay.service;

import com.example.commerce.common.BizException;
import com.example.commerce.pay.config.WechatProperties;
import com.example.commerce.pay.dto.*;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.AmountReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付实现 (JSAPI)
 */
@Slf4j
@Service
public class WechatPayService implements PayService {

    private static final String CHANNEL = "WECHAT";

    private final WechatProperties props;
    private final PayOrderMapper payOrderMapper;
    private final Config config;
    private final JsapiService jsapiService;
    private final com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension queryService;
    private final RefundService refundService;

    public WechatPayService(WechatProperties props, PayOrderMapper payOrderMapper) {
        this.props = props;
        this.payOrderMapper = payOrderMapper;
        // SDK 会自动下载平台证书
        this.config = new RSAAutoCertificateConfig(
                props.getMchId(),
                props.getCertSerialNo(),
                props.getApiV3Key(),
                props.getPrivateKey());
        this.jsapiService = new JsapiService.Builder().config(config).build();
        this.queryService = new com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension.Builder().config(config).build();
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

            // openid 需业务方传入，这里简化
            com.wechat.pay.java.service.payments.jsapi.model.Payer payer = new com.wechat.pay.java.service.payments.jsapi.model.Payer();
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
        // SDK 内置验签，parseNotify 时同步验签
        return true;
    }

    @Override
    public PayResult parseNotify(String body, Map<String, String> headers) {
        try {
            // 微信回调需要: Wechatpay-Timestamp / Wechatpay-Nonce / Wechatpay-Signature / Wechatpay-Serial
            com.wechat.pay.java.core.notification.NotificationConfig notificationConfig =
                    new com.wechat.pay.java.core.notification.RSAAutoCertificateConfig(
                            props.getMchId(),
                            props.getCertSerialNo(),
                            props.getApiV3Key(),
                            props.getPrivateKey());
            com.wechat.pay.java.core.notification.NotificationParser parser =
                    new com.wechat.pay.java.core.notification.NotificationParser(notificationConfig);
            com.wechat.pay.java.service.payments.model.Transaction tx =
                    parser.parse(body, headers, Transaction.class);

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
            Transaction tx = queryService.queryOrderByOutTradeNo(outTradeNo, props.getMchId());
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
}
