package com.example.commerce.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pay_notify_log")
public class PayNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String outTradeNo;
    private String channel;
    private String rawBody;
    private Integer processed;
    private LocalDateTime createTime;
}
