package com.example.commerce.pay.dto;

import lombok.Data;

@Data
public class PayResult {

    /** 业务订单号 */
    private String outTradeNo;
    /** 第三方交易号 */
    private String tradeNo;
    /** 支付状态 PAID/CLOSED */
    private String status;
    /** 回调原始内容(已验签)，用于返回给第三方应答 */
    private String rawBody;
}
