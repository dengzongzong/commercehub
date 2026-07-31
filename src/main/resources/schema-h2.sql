-- H2 数据库 schema (mock profile 和测试用，兼容 MySQL 语法)

CREATE TABLE IF NOT EXISTS pay_order (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    out_trade_no  VARCHAR(64)  NOT NULL,
    channel       VARCHAR(16)  NOT NULL,
    subject       VARCHAR(128) NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'INIT',
    trade_no      VARCHAR(64)           DEFAULT NULL,
    notify_time   TIMESTAMP             DEFAULT NULL,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_out_trade_no UNIQUE (out_trade_no)
);
CREATE INDEX IF NOT EXISTS idx_pay_trade_no ON pay_order(trade_no);

CREATE TABLE IF NOT EXISTS pay_notify_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    out_trade_no  VARCHAR(64)  NOT NULL,
    channel       VARCHAR(16)  NOT NULL,
    raw_body      CLOB         NOT NULL,
    processed     SMALLINT     NOT NULL DEFAULT 0,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_pnl_out_trade_no ON pay_notify_log(out_trade_no);

CREATE TABLE IF NOT EXISTS logistics_order (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(64)  NOT NULL,
    carrier       VARCHAR(32)  NOT NULL,
    tracking_no   VARCHAR(64)           DEFAULT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'INIT',
    receiver      VARCHAR(64)           DEFAULT NULL,
    phone         VARCHAR(32)           DEFAULT NULL,
    address       VARCHAR(256)          DEFAULT NULL,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_lo_order_no ON logistics_order(order_no);
CREATE INDEX IF NOT EXISTS idx_lo_tracking_no ON logistics_order(tracking_no);

CREATE TABLE IF NOT EXISTS sms_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    phone         VARCHAR(32)  NOT NULL,
    template_code VARCHAR(64)  NOT NULL,
    params        VARCHAR(512)          DEFAULT NULL,
    vendor        VARCHAR(32)  NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'INIT',
    biz_id        VARCHAR(128)          DEFAULT NULL,
    error_msg     VARCHAR(256)          DEFAULT NULL,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sms_phone ON sms_record(phone);

CREATE TABLE IF NOT EXISTS cert_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    biz_no        VARCHAR(64)  NOT NULL,
    user_id       VARCHAR(64)  NOT NULL,
    real_name     VARCHAR(64)  NOT NULL,
    cert_no       VARCHAR(32)  NOT NULL,
    certify_id    VARCHAR(64)           DEFAULT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'INIT',
    certify_url   VARCHAR(512)          DEFAULT NULL,
    biz_code      VARCHAR(16)           DEFAULT 'FACE',
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_biz_no UNIQUE (biz_no)
);
CREATE INDEX IF NOT EXISTS idx_cert_user_id ON cert_record(user_id);
CREATE INDEX IF NOT EXISTS idx_cert_certify_id ON cert_record(certify_id);
