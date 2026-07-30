package com.example.commerce.sms.service;

import com.example.commerce.sms.dto.SmsReq;
import com.example.commerce.sms.dto.SmsResp;

/**
 * 短信接口
 */
public interface SmsService {

    String vendor();

    SmsResp send(SmsReq req);
}
