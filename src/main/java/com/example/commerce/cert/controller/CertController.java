package com.example.commerce.cert.controller;

import com.example.commerce.cert.dto.CertInitializeReq;
import com.example.commerce.cert.dto.CertResult;
import com.example.commerce.cert.entity.CertRecord;
import com.example.commerce.cert.service.CertBizService;
import com.example.commerce.common.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cert")
@RequiredArgsConstructor
public class CertController {

    private final CertBizService certBizService;

    /** 发起实名认证，返回认证链接 */
    @PostMapping("/initialize")
    public Response<CertResult> initialize(@Valid @RequestBody CertInitializeReq req) {
        return Response.success(certBizService.initialize(req));
    }

    /** 查询认证结果 */
    @GetMapping("/query/{bizNo}")
    public Response<CertResult> query(@PathVariable String bizNo) {
        return Response.success(certBizService.query(bizNo));
    }

    /** 按用户ID查最近一次认证记录 */
    @GetMapping("/record/{userId}")
    public Response<CertRecord> getByUser(@PathVariable String userId) {
        return Response.success(certBizService.getByUserId(userId));
    }
}
