package com.example.commerce.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.commerce.common.BizException;
import com.example.commerce.pay.dto.*;
import com.example.commerce.pay.entity.PayNotifyLog;
import com.example.commerce.pay.entity.PayOrder;
import com.example.commerce.pay.mapper.PayNotifyLogMapper;
import com.example.commerce.pay.mapper.PayOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付业务编排：下单落库 + 回调幂等 + 退款 + 查单
 */
@Slf4j
@Service
public class PayBizService {

    private final PayRouter payRouter;
    private final PayOrderMapper payOrderMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;

    public PayBizService(PayRouter payRouter, PayOrderMapper payOrderMapper, PayNotifyLogMapper payNotifyLogMapper) {
        this.payRouter = payRouter;
        this.payOrderMapper = payOrderMapper;
        this.payNotifyLogMapper = payNotifyLogMapper;
    }

    /**
     * 发起支付：先落单，再调渠道
     */
    public PayResp pay(PayReq req) {
        // 幂等：同 outTradeNo 已存在且已支付则直接报错
        PayOrder exist = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        if (exist != null) {
            if ("PAID".equals(exist.getStatus())) {
                throw new BizException("订单已支付，不能重复支付");
            }
            // 已存在未支付单，复用
            return payRouter.of(req.getChannel()).pay(req);
        }

        PayOrder order = new PayOrder();
        order.setOutTradeNo(req.getOutTradeNo());
        order.setChannel(req.getChannel());
        order.setSubject(req.getSubject());
        order.setAmount(req.getAmount());
        order.setStatus("PAYING");
        payOrderMapper.insert(order);

        return payRouter.of(req.getChannel()).pay(req);
    }

    /**
     * 处理第三方回调：原始报文先落库，再幂等更新订单状态
     * 返回需要回写给第三方的应答内容
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(String channel, String body, java.util.Map<String, String> headers) {
        PayService service = payRouter.of(channel);

        // 1. 验签
        if (!service.verifyNotify(body, headers)) {
            log.warn("回调验签失败 channel={}", channel);
            return "fail";
        }

        // 2. 解析
        PayResult result = service.parseNotify(body, headers);

        // 3. 落库回调日志
        PayNotifyLog logEntity = new PayNotifyLog();
        logEntity.setOutTradeNo(result.getOutTradeNo());
        logEntity.setChannel(channel);
        logEntity.setRawBody(body);
        logEntity.setProcessed(0);
        payNotifyLogMapper.insert(logEntity);

        // 4. 幂等更新订单状态：基于状态机 where status = 'PAYING'
        // 只有 PAYING -> PAID 才更新，避免重复回调重复处理
        if ("PAID".equals(result.getStatus())) {
            int updated = payOrderMapper.update(null,
                    new UpdateWrapper<PayOrder>()
                            .eq("out_trade_no", result.getOutTradeNo())
                            .eq("status", "PAYING")
                            .set("status", "PAID")
                            .set("trade_no", result.getTradeNo())
                            .set("notify_time", LocalDateTime.now()));

            if (updated == 0) {
                // 已处理过 或 状态不匹配
                log.info("订单已处理过，跳过 outTradeNo={}", result.getOutTradeNo());
            } else {
                log.info("订单支付成功 outTradeNo={} tradeNo={}", result.getOutTradeNo(), result.getTradeNo());
                // TODO 这里可触发下游业务(发货/充值等)
            }
        } else if ("CLOSED".equals(result.getStatus())) {
            payOrderMapper.update(null,
                    new UpdateWrapper<PayOrder>()
                            .eq("out_trade_no", result.getOutTradeNo())
                            .eq("status", "PAYING")
                            .set("status", "CLOSED"));
        }

        // 5. 标记日志已处理
        payNotifyLogMapper.update(null,
                new UpdateWrapper<PayNotifyLog>()
                        .eq("id", logEntity.getId())
                        .set("processed", 1));

        return result.getRawBody();
    }

    public RefundResp refund(RefundReq req) {
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", req.getOutTradeNo()));
        if (order == null) {
            return RefundResp.fail("订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            return RefundResp.fail("订单状态不允许退款: " + order.getStatus());
        }
        // 状态机：PAID -> REFUNDING
        payOrderMapper.update(null,
                new UpdateWrapper<PayOrder>()
                        .eq("out_trade_no", req.getOutTradeNo())
                        .eq("status", "PAID")
                        .set("status", "REFUNDING"));

        RefundResp resp = payRouter.of(order.getChannel()).refund(req);
        if (resp.isSuccess()) {
            payOrderMapper.update(null,
                    new UpdateWrapper<PayOrder>()
                            .eq("out_trade_no", req.getOutTradeNo())
                            .set("status", "REFUNDED"));
        } else {
            payOrderMapper.update(null,
                    new UpdateWrapper<PayOrder>()
                            .eq("out_trade_no", req.getOutTradeNo())
                            .set("status", "PAID"));
        }
        return resp;
    }

    public PayResult queryTrade(String outTradeNo) {
        PayOrder order = payOrderMapper.selectOne(
                new QueryWrapper<PayOrder>().eq("out_trade_no", outTradeNo));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return payRouter.of(order.getChannel()).queryTrade(outTradeNo);
    }
}
