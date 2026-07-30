package com.example.commerce.pay.service;

import com.example.commerce.common.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付渠道路由
 */
@Component
public class PayRouter {

    private final Map<String, PayService> services;

    public PayRouter(List<PayService> all) {
        this.services = all.stream().collect(Collectors.toMap(PayService::channel, p -> p));
    }

    public PayService of(String channel) {
        PayService s = services.get(channel);
        if (s == null) {
            throw new BizException("不支持的支付渠道: " + channel);
        }
        return s;
    }
}
