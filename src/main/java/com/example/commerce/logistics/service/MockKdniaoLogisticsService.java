package com.example.commerce.logistics.service;

import com.example.commerce.logistics.dto.ExpressReq;
import com.example.commerce.logistics.dto.ExpressResp;
import com.example.commerce.logistics.dto.TraceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 快递鸟 Mock 实现
 *
 * 行为对齐真实 KdniaoLogisticsService：
 * - createOrder() 返回一个模拟的运单号（LogisticCode）
 * - queryTrace() 返回若干条时间递增的轨迹，最后一条带「签收」二字，
 *   触发 LogisticsBizService 把本地订单状态从 SHIPPED 改成 DELIVERED
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockKdniaoLogisticsService implements LogisticsService {

    @Override
    public String carrier() {
        return "KDNIAO";
    }

    @Override
    public ExpressResp createOrder(ExpressReq req) {
        String trackingNo = "SF" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("[MOCK-KDNIAO] 电子面单下单 orderNo={} carrier={} -> trackingNo={}",
                req.getOrderNo(), req.getCarrier(), trackingNo);
        return ExpressResp.ok(trackingNo);
    }

    @Override
    public TraceResult queryTrace(String trackingNo) {
        log.info("[MOCK-KDNIAO] 查询物流轨迹 trackingNo={}", trackingNo);

        LocalDateTime base = LocalDateTime.now().minusDays(2);
        List<TraceResult.TraceItem> items = new ArrayList<>();
        items.add(item(base, "快件已从【上海浦东分拨中心】发出"));
        items.add(item(base.plusHours(6), "快件已到达【杭州转运中心】"));
        items.add(item(base.plusHours(12), "快件已由【杭州西湖区营业点】派送中，派件员：张三 电话：13800138000"));
        items.add(item(base.plusHours(20), "快件已签收，签收人：本人"));

        TraceResult r = new TraceResult();
        r.setTrackingNo(trackingNo);
        r.setTraces(items);
        r.setLastTrace(items.get(items.size() - 1).getContent());
        return r;
    }

    private TraceResult.TraceItem item(LocalDateTime time, String content) {
        TraceResult.TraceItem it = new TraceResult.TraceItem();
        it.setTime(time);
        it.setContent(content);
        return it;
    }
}
