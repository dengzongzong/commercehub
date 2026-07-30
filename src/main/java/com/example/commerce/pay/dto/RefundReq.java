package com.example.commerce.pay.dto;

import lombok.Data;

@Data
public class RefundReq {

    private String outTradeNo;
    private String refundNo;
    /** 退款金额，为空则全额退款 */
    private java.math.BigDecimal refundAmount;
    private String reason;
}
