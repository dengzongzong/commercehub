package com.example.commerce.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpressReq {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** 快递公司编码 SF/YTO/ZTO... */
    @NotBlank(message = "快递公司不能为空")
    private String carrier;

    @NotBlank(message = "收件人不能为空")
    private String receiver;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "地址不能为空")
    private String address;
}
