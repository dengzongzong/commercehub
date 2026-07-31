package com.example.commerce.logistics;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.commerce.AbstractMockIntegrationTest;
import com.example.commerce.common.Response;
import com.example.commerce.logistics.dto.ExpressReq;
import com.example.commerce.logistics.dto.ExpressResp;
import com.example.commerce.logistics.dto.TraceResult;
import com.example.commerce.logistics.entity.LogisticsOrder;
import com.example.commerce.logistics.mapper.LogisticsOrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 物流全流程集成测试（Mock 模式）
 *
 * 覆盖：电子面单下单 -> 落库 -> 查轨迹 -> 签收后状态自动流转
 */
@DisplayName("物流模块-集成测试")
class LogisticsIntegrationTest extends AbstractMockIntegrationTest {

    @Autowired
    private LogisticsOrderMapper logisticsOrderMapper;

    @Test
    @DisplayName("下单 -> 查轨迹 -> 签收状态流转")
    void createAndTrace() {
        // ====== 1. 电子面单下单 ======
        ExpressReq req = new ExpressReq();
        req.setOrderNo("TEST_LOGI_" + System.currentTimeMillis());
        req.setCarrier("SF");
        req.setReceiver("张三");
        req.setPhone("13800138000");
        req.setAddress("浙江省杭州市西湖区文三路 123 号");

        Response<ExpressResp> resp = postJson("/logistics/create", req,
                new ParameterizedTypeReference<Response<ExpressResp>>() {}).getBody();
        assertNotNull(resp);
        assertEquals(0, resp.getCode(), "下单应返回成功");
        // Mock 返回的运单号格式与快递鸟 LogisticCode 一致
        assertNotNull(resp.getData());
        assertNotNull(resp.getData().getTrackingNo(), "应返回运单号");

        // 落库校验
        LogisticsOrder order = logisticsOrderMapper.selectOne(
                new QueryWrapper<LogisticsOrder>().eq("order_no", req.getOrderNo()));
        assertNotNull(order, "下单后应落库");
        assertEquals("SHIPPED", order.getStatus(), "初始状态应为 SHIPPED");
        assertNotNull(order.getTrackingNo(), "应保存运单号");

        // ====== 2. 查物流轨迹 ======
        Response<TraceResult> traceResp = getJson("/logistics/trace/" + order.getTrackingNo(),
                new ParameterizedTypeReference<Response<TraceResult>>() {}).getBody();
        assertNotNull(traceResp);
        assertEquals(0, traceResp.getCode());
        assertNotNull(traceResp.getData());
        assertNotNull(traceResp.getData().getTraces(), "应返回轨迹列表");
        assertFalse(traceResp.getData().getTraces().isEmpty(), "轨迹列表不应为空");

        // ====== 3. 签收状态流转：Mock 轨迹最后一条带「签收」二字，应触发 SHIPPED -> DELIVERED ======
        LogisticsOrder delivered = logisticsOrderMapper.selectOne(
                new QueryWrapper<LogisticsOrder>().eq("order_no", req.getOrderNo()));
        assertEquals("DELIVERED", delivered.getStatus(),
                "查到签收轨迹后，状态应自动流转为 DELIVERED");

        // ====== 4. 按订单号查运单 ======
        Response<LogisticsOrder> orderByNo = getJson("/logistics/order/" + req.getOrderNo(),
                new ParameterizedTypeReference<Response<LogisticsOrder>>() {}).getBody();
        assertNotNull(orderByNo);
        assertEquals(0, orderByNo.getCode());
    }
}
