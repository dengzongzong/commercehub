package com.example.commerce.logistics.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.commerce.common.BizException;
import com.example.commerce.logistics.config.KdniaoProperties;
import com.example.commerce.logistics.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 快递鸟聚合实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "false", matchIfMissing = true)
public class KdniaoLogisticsService implements LogisticsService {

    private static final String CARRIER = "KDNIAO";
    /** 即时查询接口 RequestType */
    private static final int REQ_TYPE = 1002;
    /** 电子面单接口 RequestType */
    private static final int ORDER_TYPE = 1007;

    private final KdniaoProperties props;

    public KdniaoLogisticsService(KdniaoProperties props) {
        this.props = props;
    }

    @Override
    public String carrier() {
        return CARRIER;
    }

    @Override
    public ExpressResp createOrder(ExpressReq req) {
        try {
            JSONObject requestData = new JSONObject();
            requestData.set("OrderCode", req.getOrderNo());
            requestData.set("ShipperCode", req.getCarrier());
            requestData.set("ReceiverName", req.getReceiver());
            requestData.set("ReceiverTel", req.getPhone());
            requestData.set("ReceiverAddress", req.getAddress());

            JSONObject resp = doPost(ORDER_TYPE, requestData.toString());

            if (resp.getBool("Success")) {
                String logisticCode = resp.getStr("LogisticCode");
                return ExpressResp.ok(logisticCode);
            }
            return ExpressResp.fail(resp.getStr("Reason"));
        } catch (Exception e) {
            log.error("快递鸟下单失败", e);
            throw new BizException("物流下单失败: " + e.getMessage());
        }
    }

    @Override
    public TraceResult queryTrace(String trackingNo) {
        try {
            JSONObject requestData = new JSONObject();
            requestData.set("LogisticCode", trackingNo);

            JSONObject resp = doPost(REQ_TYPE, requestData.toString());

            TraceResult result = new TraceResult();
            result.setTrackingNo(trackingNo);

            JSONArray traces = resp.getJSONArray("Traces");
            List<TraceResult.TraceItem> items = new ArrayList<>();
            if (traces != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (Object o : traces) {
                    JSONObject t = (JSONObject) o;
                    TraceResult.TraceItem item = new TraceResult.TraceItem();
                    item.setTime(LocalDateTime.parse(t.getStr("AcceptTime"), fmt));
                    item.setContent(t.getStr("AcceptStation"));
                    items.add(item);
                }
            }
            result.setTraces(items);
            if (!items.isEmpty()) {
                result.setLastTrace(items.get(items.size() - 1).getContent());
            }
            return result;
        } catch (Exception e) {
            log.error("快递鸟查询轨迹失败 trackingNo={}", trackingNo, e);
            throw new BizException("查询物流轨迹失败: " + e.getMessage());
        }
    }

    /**
     * 快递鸟请求：DataSign = Base64(MD5(RequestData + ApiKey), UTF-8)
     */
    private JSONObject doPost(int requestType, String requestData) {
        String dataSign = SecureUtil.md5(requestData + props.getApiKey());
        String sign = java.util.Base64.getEncoder().encodeToString(dataSign.getBytes(StandardCharsets.UTF_8));

        java.util.HashMap<String, Object> form = new java.util.HashMap<>();
        form.put("RequestData", URLEncoder.encode(requestData, StandardCharsets.UTF_8));
        form.put("EBusinessID", props.getEbusinessId());
        form.put("RequestType", requestType);
        form.put("DataSign", URLEncoder.encode(sign, StandardCharsets.UTF_8));
        form.put("DataType", "2");

        String respBody = HttpUtil.post(props.getProdUrl(), form, 10_000);
        log.debug("快递鸟响应: {}", respBody);
        return JSONUtil.parseObj(respBody);
    }
}
