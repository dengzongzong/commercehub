package com.example.commerce.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_order")
public class PayOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String outTradeNo;
    private String channel;
    private String subject;
    private BigDecimal amount;
    private String status;
    private String tradeNo;
    private LocalDateTime notifyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
