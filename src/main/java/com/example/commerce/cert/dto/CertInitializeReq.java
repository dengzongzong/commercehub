package com.example.commerce.cert.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertInitializeReq {

    /** 业务方用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 身份证号 */
    @NotBlank(message = "身份证号不能为空")
    private String certNo;

    /** 认证方式 FACE(人脸)/SMART(多因子)，默认 FACE */
    private String bizCode = "FACE";
}
