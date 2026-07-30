package com.example.commerce.pay.controller;

import com.example.commerce.common.Response;
import com.example.commerce.pay.dto.PayReq;
import com.example.commerce.pay.dto.PayResp;
import com.example.commerce.pay.dto.PayResult;
import com.example.commerce.pay.dto.RefundReq;
import com.example.commerce.pay.dto.RefundResp;
import com.example.commerce.pay.service.PayBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayBizService payBizService;

    /** 发起支付 */
    @PostMapping("/pay")
    public Response<PayResp> pay(@Valid @RequestBody PayReq req) {
        return Response.success(payBizService.pay(req));
    }

    /** 支付宝异步回调 */
    @PostMapping(value = "/notify/alipay", produces = "text/plain;charset=UTF-8")
    public String alipayNotify(HttpServletRequest request) {
        String body = readBody(request);
        Map<String, String> headers = collectHeaders(request);
        return payBizService.handleNotify("ALIPAY", body, headers);
    }

    /** 微信异步回调 */
    @PostMapping(value = "/notify/wechat", produces = "application/json")
    public String wechatNotify(HttpServletRequest request) {
        String body = readBody(request);
        Map<String, String> headers = collectHeaders(request);
        return payBizService.handleNotify("WECHAT", body, headers);
    }

    /** 退款 */
    @PostMapping("/refund")
    public Response<RefundResp> refund(@RequestBody RefundReq req) {
        return Response.success(payBizService.refund(req));
    }

    /** 主动查单 */
    @GetMapping("/query/{outTradeNo}")
    public Response<PayResult> query(@PathVariable String outTradeNo) {
        return Response.success(payBizService.queryTrade(outTradeNo));
    }

    private String readBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("读取请求体失败", e);
            return "";
        }
    }

    private Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
