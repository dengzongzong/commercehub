package com.example.commerce.cert.service;

/**
 * 实名认证客户端接口
 */
public interface CertClient {

    /** 初始化认证，返回 certify_id */
    String initialize(String outerOrderNo, String bizCode, String realName, String certNo);

    /** 生成认证页面URL */
    String generateCertifyUrl(String certifyId);

    /** 查询认证结果，返回 true=通过 */
    boolean query(String certifyId);
}
