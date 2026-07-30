package com.example.commerce.logistics.service;

import com.example.commerce.logistics.dto.*;

/**
 * 物流接口
 */
public interface LogisticsService {

    String carrier();

    /** 电子面单下单 */
    ExpressResp createOrder(ExpressReq req);

    /** 查询物流轨迹 */
    TraceResult queryTrace(String trackingNo);
}
