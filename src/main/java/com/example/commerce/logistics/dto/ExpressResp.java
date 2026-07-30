package com.example.commerce.logistics.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExpressResp {

    /** 快递公司运单号 */
    private String trackingNo;
    /** 是否下单成功 */
    private boolean success;
    private String message;

    public static ExpressResp ok(String trackingNo) {
        ExpressResp r = new ExpressResp();
        r.success = true;
        r.trackingNo = trackingNo;
        return r;
    }

    public static ExpressResp fail(String message) {
        ExpressResp r = new ExpressResp();
        r.success = false;
        r.message = message;
        return r;
    }
}
