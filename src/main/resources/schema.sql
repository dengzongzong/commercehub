-- 支付单
CREATE TABLE IF NOT EXISTS `pay_order` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `out_trade_no`  VARCHAR(64)  NOT NULL COMMENT '业务订单号',
    `channel`       VARCHAR(16)  NOT NULL COMMENT '支付渠道 ALIPAY/WECHAT',
    `subject`       VARCHAR(128) NOT NULL COMMENT '订单标题',
    `amount`        DECIMAL(12,2) NOT NULL COMMENT '金额(元)',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PAYING/PAID/REFUNDING/REFUNDED/CLOSED',
    `trade_no`      VARCHAR(64)           DEFAULT NULL COMMENT '第三方交易号',
    `notify_time`   DATETIME              DEFAULT NULL COMMENT '回调时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
    KEY `idx_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';

-- 支付回调日志(幂等用)
CREATE TABLE IF NOT EXISTS `pay_notify_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `out_trade_no`  VARCHAR(64)  NOT NULL,
    `channel`       VARCHAR(16)  NOT NULL,
    `raw_body`      TEXT         NOT NULL COMMENT '原始报文',
    `processed`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_out_trade_no` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志';

-- 物流运单
CREATE TABLE IF NOT EXISTS `logistics_order` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `order_no`      VARCHAR(64)  NOT NULL COMMENT '业务订单号',
    `carrier`       VARCHAR(32)  NOT NULL COMMENT '快递公司编码 SF/YTO...',
    `tracking_no`   VARCHAR(64)           DEFAULT NULL COMMENT '运单号',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'INIT' COMMENT 'INIT/SHIPPED/DELIVERED',
    `receiver`      VARCHAR(64)           DEFAULT NULL COMMENT '收件人',
    `phone`         VARCHAR(32)           DEFAULT NULL COMMENT '收件电话',
    `address`       VARCHAR(256)          DEFAULT NULL COMMENT '收件地址',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_tracking_no` (`tracking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流运单';

-- 短信发送记录
CREATE TABLE IF NOT EXISTS `sms_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `phone`         VARCHAR(32)  NOT NULL COMMENT '手机号',
    `template_code` VARCHAR(64)  NOT NULL COMMENT '模板ID',
    `params`        VARCHAR(512)          DEFAULT NULL COMMENT '模板参数JSON',
    `vendor`        VARCHAR(32)  NOT NULL COMMENT '厂商 ALIYUN',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'INIT' COMMENT 'INIT/SUCCESS/FAIL',
    `biz_id`        VARCHAR(128)          DEFAULT NULL COMMENT '回执ID',
    `error_msg`     VARCHAR(256)          DEFAULT NULL COMMENT '失败原因',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录';

-- 实名认证记录
CREATE TABLE IF NOT EXISTS `cert_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `biz_no`        VARCHAR(64)  NOT NULL COMMENT '业务流水号',
    `user_id`       VARCHAR(64)  NOT NULL COMMENT '业务方用户ID',
    `real_name`     VARCHAR(64)  NOT NULL COMMENT '真实姓名',
    `cert_no`       VARCHAR(32)  NOT NULL COMMENT '身份证号',
    `certify_id`    VARCHAR(64)           DEFAULT NULL COMMENT '支付宝认证流水号',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PROCESSING/PASSED/FAILED',
    `certify_url`   VARCHAR(512)          DEFAULT NULL COMMENT '认证页面URL',
    `biz_code`      VARCHAR(16)           DEFAULT 'FACE' COMMENT '认证方式 FACE/SMART',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_no` (`biz_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_certify_id` (`certify_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实名认证记录';

