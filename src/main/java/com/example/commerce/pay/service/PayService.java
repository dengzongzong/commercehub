package com.example.commerce.pay.service;

import com.example.commerce.pay.dto.*;

/**
 * 统一支付接口
 */
public interface PayService {

    /** 渠道标识 ALIPAY / WECHAT */
    String channel();

    /** 统一下单 */
    PayResp pay(PayReq req);

    /** 回调验签，返回验签后的原始内容 */
    boolean verifyNotify(String body, java.util.Map<String, String> headers);

    /** 解析回调内容 */
    PayResult parseNotify(String body, java.util.Map<String, String> headers);

    /** 退款 */
    RefundResp refund(RefundReq req);

    /** 主动查单 */
    PayResult queryTrade(String outTradeNo);
}
