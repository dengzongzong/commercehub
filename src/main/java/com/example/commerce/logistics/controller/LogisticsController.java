package com.example.commerce.logistics.controller;

import com.example.commerce.common.Response;
import com.example.commerce.logistics.dto.ExpressReq;
import com.example.commerce.logistics.dto.ExpressResp;
import com.example.commerce.logistics.dto.TraceResult;
import com.example.commerce.logistics.entity.LogisticsOrder;
import com.example.commerce.logistics.service.LogisticsBizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsBizService logisticsBizService;

    /** 下单(电子面单) */
    @PostMapping("/create")
    public Response<ExpressResp> create(@Valid @RequestBody ExpressReq req) {
        return Response.success(logisticsBizService.createOrder(req));
    }

    /** 查物流轨迹 */
    @GetMapping("/trace/{trackingNo}")
    public Response<TraceResult> trace(@PathVariable String trackingNo) {
        return Response.success(logisticsBizService.queryTrace(trackingNo));
    }

    /** 按订单号查运单 */
    @GetMapping("/order/{orderNo}")
    public Response<LogisticsOrder> get(@PathVariable String orderNo) {
        return Response.success(logisticsBizService.getByOrderNo(orderNo));
    }
}
