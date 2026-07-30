package com.example.commerce.sms.controller;

import com.example.commerce.common.Response;
import com.example.commerce.sms.dto.SmsReq;
import com.example.commerce.sms.dto.SmsResp;
import com.example.commerce.sms.service.SmsBizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsBizService smsBizService;

    /** 发送短信 */
    @PostMapping("/send")
    public Response<SmsResp> send(@Valid @RequestBody SmsReq req) {
        return Response.success(smsBizService.send(req));
    }
}
