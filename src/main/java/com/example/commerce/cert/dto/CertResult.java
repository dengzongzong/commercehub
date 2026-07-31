package com.example.commerce.cert.dto;

import lombok.Data;

@Data
public class CertResult {

    /** 业务流水号 */
    private String bizNo;
    /** 支付宝认证流水号 */
    private String certifyId;
    /** 认证页面URL，前端跳转引导用户认证 */
    private String certifyUrl;
    /** INIT/PROCESSING/PASSED/FAILED */
    private String status;
    private String message;

    public static CertResult of(String bizNo, String certifyId, String certifyUrl) {
        CertResult r = new CertResult();
        r.bizNo = bizNo;
        r.certifyId = certifyId;
        r.certifyUrl = certifyUrl;
        r.status = "PROCESSING";
        return r;
    }
}
