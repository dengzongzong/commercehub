package com.example.commerce.pay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayReq {

    @NotBlank(message = "订单号不能为空")
    private String outTradeNo;

    @NotBlank(message = "渠道不能为空")
    private String channel;

    @NotBlank(message = "标题不能为空")
    private String subject;

    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    /** 微信JSAPI支付必填，支付宝不需要 */
    private String openid;
}
