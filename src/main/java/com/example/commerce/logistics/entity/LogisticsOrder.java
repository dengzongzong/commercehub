package com.example.commerce.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_order")
public class LogisticsOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private String carrier;
    private String trackingNo;
    private String status;
    private String receiver;
    private String phone;
    private String address;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
