package com.example.commerce.cert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实名认证记录
 */
@Data
@TableName("cert_record")
public class CertRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务流水号(本系统生成) */
    private String bizNo;
    /** 业务方用户标识 */
    private String userId;
    /** 真实姓名 */
    private String realName;
    /** 身份证号 */
    private String certNo;
    /** 支付宝认证流水号 certify_id */
    private String certifyId;
    /** INIT/PROCESSING/PASSED/FAILED */
    private String status;
    /** 认证页面URL */
    private String certifyUrl;
    /** 认证方式 FACE/SMART */
    private String bizCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
