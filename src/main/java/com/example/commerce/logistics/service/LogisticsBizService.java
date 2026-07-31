package com.example.commerce.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.commerce.common.BizException;
import com.example.commerce.logistics.dto.ExpressReq;
import com.example.commerce.logistics.dto.ExpressResp;
import com.example.commerce.logistics.dto.TraceResult;
import com.example.commerce.logistics.entity.LogisticsOrder;
import com.example.commerce.logistics.mapper.LogisticsOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 物流业务编排：下单落库 + 查轨迹
 */
@Slf4j
@Service
public class LogisticsBizService {

    private final LogisticsService logisticsService;
    private final LogisticsOrderMapper logisticsOrderMapper;

    public LogisticsBizService(LogisticsService logisticsService,
                               LogisticsOrderMapper logisticsOrderMapper) {
        this.logisticsService = logisticsService;
        this.logisticsOrderMapper = logisticsOrderMapper;
    }

    public ExpressResp createOrder(ExpressReq req) {
        ExpressResp resp = logisticsService.createOrder(req);
        if (!resp.isSuccess()) {
            return resp;
        }
        LogisticsOrder order = new LogisticsOrder();
        order.setOrderNo(req.getOrderNo());
        order.setCarrier(req.getCarrier());
        order.setTrackingNo(resp.getTrackingNo());
        order.setStatus("SHIPPED");
        order.setReceiver(req.getReceiver());
        order.setPhone(req.getPhone());
        order.setAddress(req.getAddress());
        logisticsOrderMapper.insert(order);
        return resp;
    }

    public TraceResult queryTrace(String trackingNo) {
        if (trackingNo == null || trackingNo.isEmpty()) {
            throw new BizException("运单号不能为空");
        }
        TraceResult result = logisticsService.queryTrace(trackingNo);

        // 更新本地状态
        if (result.getLastTrace() != null && result.getLastTrace().contains("签收")) {
            logisticsOrderMapper.update(null,
                    new UpdateWrapper<LogisticsOrder>()
                            .eq("tracking_no", trackingNo)
                            .eq("status", "SHIPPED")
                            .set("status", "DELIVERED"));
        }
        return result;
    }

    public LogisticsOrder getByOrderNo(String orderNo) {
        return logisticsOrderMapper.selectOne(
                new QueryWrapper<LogisticsOrder>().eq("order_no", orderNo));
    }
}
