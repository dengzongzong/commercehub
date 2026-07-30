package com.example.commerce.pay.dto;

import lombok.Data;

@Data
public class RefundResp {

    private boolean success;
    private String refundId;
    private String message;

    public static RefundResp ok(String refundId) {
        RefundResp r = new RefundResp();
        r.success = true;
        r.refundId = refundId;
        return r;
    }

    public static RefundResp fail(String message) {
        RefundResp r = new RefundResp();
        r.success = false;
        r.message = message;
        return r;
    }
}
