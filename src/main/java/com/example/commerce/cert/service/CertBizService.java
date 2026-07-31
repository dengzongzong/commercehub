package com.example.commerce.cert.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.commerce.cert.dto.CertInitializeReq;
import com.example.commerce.cert.dto.CertResult;
import com.example.commerce.cert.entity.CertRecord;
import com.example.commerce.cert.mapper.CertRecordMapper;
import com.example.commerce.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 实名认证业务编排
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertBizService {

    private final AlipayCertClient alipayCertClient;
    private final CertRecordMapper certRecordMapper;

    /**
     * 发起认证：初始化 + 生成链接 + 落库
     */
    public CertResult initialize(CertInitializeReq req) {
        String bizNo = "CERT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);

        // 1. 调支付宝初始化
        String certifyId = alipayCertClient.initialize(bizNo, req.getBizCode(), req.getRealName(), req.getCertNo());

        // 2. 生成认证URL
        String certifyUrl = alipayCertClient.generateCertifyUrl(certifyId);

        // 3. 落库
        CertRecord record = new CertRecord();
        record.setBizNo(bizNo);
        record.setUserId(req.getUserId());
        record.setRealName(req.getRealName());
        record.setCertNo(req.getCertNo());
        record.setCertifyId(certifyId);
        record.setCertifyUrl(certifyUrl);
        record.setBizCode(req.getBizCode());
        record.setStatus("PROCESSING");
        certRecordMapper.insert(record);

        log.info("实名认证已发起 bizNo={} certifyId={}", bizNo, certifyId);
        return CertResult.of(bizNo, certifyId, certifyUrl);
    }

    /**
     * 查询认证结果：调支付宝查 + 更新本地状态
     */
    public CertResult query(String bizNo) {
        CertRecord record = certRecordMapper.selectOne(
                new QueryWrapper<CertRecord>().eq("biz_no", bizNo));
        if (record == null) {
            throw new BizException("认证记录不存在");
        }

        // 已终态直接返回
        if ("PASSED".equals(record.getStatus()) || "FAILED".equals(record.getStatus())) {
            CertResult r = new CertResult();
            r.setBizNo(record.getBizNo());
            r.setCertifyId(record.getCertifyId());
            r.setCertifyUrl(record.getCertifyUrl());
            r.setStatus(record.getStatus());
            return r;
        }

        // 调支付宝查询
        boolean passed = alipayCertClient.query(record.getCertifyId());
        String finalStatus = passed ? "PASSED" : "FAILED";

        // 幂等更新：PROCESSING -> 终态
        certRecordMapper.update(null,
                new UpdateWrapper<CertRecord>()
                        .eq("biz_no", bizNo)
                        .eq("status", "PROCESSING")
                        .set("status", finalStatus)
                        .set("update_time", LocalDateTime.now()));

        CertResult r = new CertResult();
        r.setBizNo(record.getBizNo());
        r.setCertifyId(record.getCertifyId());
        r.setCertifyUrl(record.getCertifyUrl());
        r.setStatus(finalStatus);
        r.setMessage(passed ? "认证通过" : "认证未通过");
        return r;
    }

    public CertRecord getByUserId(String userId) {
        return certRecordMapper.selectOne(
                new QueryWrapper<CertRecord>()
                        .eq("user_id", userId)
                        .orderByDesc("id")
                        .last("LIMIT 1"));
    }
}
