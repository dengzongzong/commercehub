package com.example.commerce.sms.service;

import com.example.commerce.sms.dto.SmsReq;
import com.example.commerce.sms.dto.SmsResp;
import com.example.commerce.sms.entity.SmsRecord;
import com.example.commerce.sms.mapper.SmsRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信业务编排：发送 + 落库
 */
@Slf4j
@Service
public class SmsBizService {

    private final AliyunSmsService smsService;
    private final SmsRecordMapper smsRecordMapper;

    public SmsBizService(AliyunSmsService smsService, SmsRecordMapper smsRecordMapper) {
        this.smsService = smsService;
        this.smsRecordMapper = smsRecordMapper;
    }

    public SmsResp send(SmsReq req) {
        SmsResp resp = smsService.send(req);

        // 落库记录
        SmsRecord record = new SmsRecord();
        record.setPhone(req.getPhone());
        record.setTemplateCode(req.getTemplateCode());
        record.setParams(req.getParams());
        record.setVendor(smsService.vendor());
        record.setStatus(resp.isSuccess() ? "SUCCESS" : "FAIL");
        record.setBizId(resp.getBizId());
        record.setErrorMsg(resp.getMessage());
        smsRecordMapper.insert(record);

        return resp;
    }
}
