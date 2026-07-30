package com.example.commerce.sms.dto;

import lombok.Data;

@Data
public class SmsResp {

    private boolean success;
    private String bizId;
    private String message;

    public static SmsResp ok(String bizId) {
        SmsResp r = new SmsResp();
        r.success = true;
        r.bizId = bizId;
        return r;
    }

    public static SmsResp fail(String message) {
        SmsResp r = new SmsResp();
        r.success = false;
        r.message = message;
        return r;
    }
}
