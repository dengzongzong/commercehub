package com.example.commerce.pay.dto;

import lombok.Data;

@Data
public class PayResp {

    /** 支付参数(支付宝表单/微信prepay_id等)，前端拉起支付用 */
    private String payBody;
    /** 第三方交易号 */
    private String tradeNo;

    public static PayResp of(String payBody) {
        PayResp r = new PayResp();
        r.payBody = payBody;
        return r;
    }
}
