package com.example.commerce.sms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sms_record")
public class SmsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;
    private String templateCode;
    private String params;
    private String vendor;
    private String status;
    private String bizId;
    private String errorMsg;
    private LocalDateTime createTime;
}
