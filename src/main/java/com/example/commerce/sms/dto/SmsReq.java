package com.example.commerce.sms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsReq {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "模板Code不能为空")
    private String templateCode;

    /** 模板参数 JSON 字符串，如 {"code":"1234"} */
    private String params;
}
