# CommerceHub 服务开通与配置指南

CommerceHub 集成了支付（支付宝/微信）、物流（快递鸟）、短信（阿里云）、实名认证（支付宝）四类能力。本文档说明各项第三方服务的申请开通流程与配置方法。

## 目录

- [一、环境变量汇总](#一环境变量汇总)
- [二、支付宝（支付 + 实名认证）](#二支付宝支付--实名认证)
- [三、微信支付](#三微信支付)
- [四、快递鸟（物流）](#四快递鸟物流)
- [五、阿里云短信](#五阿里云短信)
- [六、MySQL](#六mysql)
- [七、本地启动](#七本地启动)
- [八、接口清单](#八接口清单)

---

## 一、环境变量汇总

部署时需配置以下环境变量（开发阶段可在 `application.yml` 直接填值，生产建议用环境变量）：

```bash
# ====== MySQL ======
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=commerce
MYSQL_USER=root
MYSQL_PASSWORD=你的数据库密码

# ====== 支付宝（支付 + 实名认证共用一个应用）======
ALIPAY_APP_ID=你的应用APPID
ALIPAY_PRIVATE_KEY=应用私钥
ALIPAY_PUBLIC_KEY=支付宝公钥
ALIPAY_GATEWAY=https://openapi.alipay.com/gateway.do   # 沙箱用 https://openapi.alipaydev.com/gateway.do
ALIPAY_NOTIFY_URL=https://你的域名/api/pay/notify/alipay
ALIPAY_RETURN_URL=https://你的域名/pay/return
CERT_RETURN_URL=https://你的域名/cert/result

# ====== 微信支付 ======
WECHAT_APP_ID=小程序/公众号AppID
WECHAT_MCH_ID=商户号
WECHAT_API_V3_KEY=APIv3密钥(32位)
WECHAT_CERT_SERIAL_NO=证书序列号
WECHAT_PRIVATE_KEY=商户私钥(apiclient_key.pem内容)
WECHAT_NOTIFY_URL=https://你的域名/api/pay/notify/wechat

# ====== 快递鸟 ======
KDNIAO_EBUSINESS_ID=商户ID
KDNIAO_API_KEY=API密钥

# ====== 阿里云短信 ======
ALIYUN_AK=AccessKey ID
ALIYUN_SK=AccessKey Secret
ALIYUN_SIGN=短信签名
```

---

## 二、支付宝（支付 + 实名认证）

支付宝的「支付」与「实名认证」共用同一个支付宝应用，只是签约的能力不同。

### 申请入口

- 正式环境：https://open.alipay.com/
- 沙箱环境（开发联调）：https://openhome.alipay.com/develop/sandbox/app

### 开通步骤

1. **注册账号**
   - 企业支付宝账号（需营业执照、对公账户）
   - 开发阶段可用沙箱账号，注册个人支付宝后在开放平台开通沙箱

2. **创建应用**
   - 开放平台 → 控制台 → 创建应用，选择「网页&移动应用」
   - 创建后获得 **APPID** → `ALIPAY_APP_ID`

3. **签约能力**（在应用「能力列表」中添加）
   - **手机网站支付** `alipay.trade.wap.pay` —— 用于支付
   - **支付宝身份认证** `alipay.user.certify.open.*` —— 用于实名认证
   - 沙箱默认已开通，正式环境需审核

4. **配置密钥（RSA2）**
   - 下载支付宝密钥生成工具：https://opendocs.alipay.com/common/02kipl
   - 生成密钥对：
     - **应用私钥** → `ALIPAY_PRIVATE_KEY`
     - **应用公钥** 上传到支付宝后台（应用详情 → 开发设置 → 接口加签方式）
   - 上传公钥后，支付宝后台会显示 **支付宝公钥** → `ALIPAY_PUBLIC_KEY`

5. **配置回调地址**
   - 支付异步回调：`ALIPAY_NOTIFY_URL`，指向 `/api/pay/notify/alipay`，需公网可达
   - 实名认证回跳：`CERT_RETURN_URL`，认证完成后浏览器跳转的页面

### 审核时长

- 支付能力：企业资质，1-3 工作日
- 实名认证：敏感能力，需业务场景说明，1-3 工作日
- 建议先用沙箱联调

### 沙箱与正式环境差异

| 项 | 沙箱 | 正式 |
|----|------|------|
| Gateway | `https://openapi.alipaydev.com/gateway.do` | `https://openapi.alipay.com/gateway.do` |
| APPID | 沙箱APPID（开放平台查看） | 正式APPID |
| 密钥 | 沙箱密钥（开放平台查看） | 正式密钥 |
| 买家账号 | 沙箱提供测试买家 | 真实支付宝账号 |

---

## 三、微信支付

### 申请入口

https://pay.weixin.qq.com/

### 开通步骤

1. **注册商户号**
   - 准备：营业执照、对公账户、法人身份证、经营场景
   - 审核约 1-5 个工作日

2. **开通支付产品**
   - 商户平台 → 产品中心 → 我的产品 → 开通 **JSAPI支付**（或 Native / APP / H5）
   - JSAPI 支付用于小程序/公众号内支付

3. **关联 AppID**
   - 商户平台 → 产品中心 → AppID 账号管理，绑定你的小程序/公众号 AppID
   - → `WECHAT_APP_ID`

4. **设置 APIv3 密钥**
   - 账户中心 → API安全 → 设置 APIv3 密钥（32 位字符串）
   - → `WECHAT_API_V3_KEY`

5. **申请 API 证书**
   - 账户中心 → API安全 → 申请 API 证书
   - 下载证书工具，生成证书文件：`apiclient_cert.pem`、`apiclient_key.pem`
   - 证书序列号在 API 安全页查看 → `WECHAT_CERT_SERIAL_NO`
   - `apiclient_key.pem` 的完整内容 → `WECHAT_PRIVATE_KEY`

### 注意事项

- **JSAPI 支付必须传 openid**：调用 `/api/pay/pay` 时，`PayReq.openid` 字段必填（用户在该 AppID 下的 openid）
- 回调地址 `WECHAT_NOTIFY_URL` 指向 `/api/pay/notify/wechat`，需公网可达、HTTPS
- SDK（wechatpay-java）会自动下载平台证书，无需手动配置

---

## 四、快递鸟（物流）

### 申请入口

https://www.kdniao.com/

### 开通步骤

1. **注册账号**
   - 支持个人/企业注册，注册后完成实名认证

2. **获取商户凭证**
   - 用户中心 → 我的信息，查看：
     - **商户ID（EBusinessID）** → `KDNIAO_EBUSINESS_ID`
     - **API Key** → `KDNIAO_API_KEY`

3. **开通接口服务**
   - 用户中心 → API接入，开通所需接口：
     - **即时查询接口**（RequestType=1002）—— 查物流轨迹，免费
     - **电子面单接口**（RequestType=1007）—— 下单取运单号，按次计费（约 0.05 元/次，需充值）
   - 电子面单需配置快递公司编码，常用编码：
     - SF 顺丰、YTO 圆通、ZTO 中通、STO 申通、YD 韵达、JD 京东、EMS

### 替代方案

若快递鸟不合适，可考虑：
- 快递100：https://www.kuaidi100.com/
- 菜鸟电子面单：https://www.cainiao.com/

---

## 五、阿里云短信

### 申请入口

https://dysms.console.aliyun.com/

### 开通步骤

1. **开通短信服务**
   - 阿里云账号实名认证后，控制台搜索「短信服务」，免费开通

2. **创建 AccessKey**（推荐 RAM 子账号）
   - 阿里云控制台 → 访问控制(RAM) → 用户 → 创建用户
   - 授予权限：`AliyunDysmsFullAccess`
   - 创建 AccessKey，记录：
     - **AccessKey ID** → `ALIYUN_AK`
     - **AccessKey Secret** → `ALIYUN_SK`
   - 安全建议：不要用主账号 AccessKey

3. **申请短信签名**
   - 短信服务控制台 → 国内消息 → 签名管理 → 添加签名
   - 签名类型：企业全称/网站/APP等，需对应资质
   - 审核约 2 小时-2 个工作日
   - → `ALIYUN_SIGN`

4. **申请短信模板**
   - 签名管理 → 模板管理 → 添加模板
   - 模板类型：验证码 / 通知 / 推广
   - 模板内容示例：`您的验证码是${code}，5分钟内有效`
   - 审核通过后获得 **模板CODE**（如 `SMS_12345678`）→ 调用时传入

### 调用方式

调用 `/api/sms/send`：

```json
{
  "phone": "13800138000",
  "templateCode": "SMS_12345678",
  "params": "{\"code\":\"1234\"}"
}
```

`params` 是模板变量的 JSON 字符串，变量名与模板占位符对应。

---

## 六、MySQL

### 本地安装

- 下载 MySQL 8：https://dev.mysql.com/downloads/
- 或用 Docker：`docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=xxx -p 3306:3306 mysql:8`

### 建库

```sql
CREATE DATABASE commerce DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

表结构由 `src/main/resources/schema.sql` 在应用启动时自动创建（`spring.sql.init.mode=always`），无需手动建表。

### 云数据库

- 阿里云 RDS：https://www.aliyun.com/product/rds
- 腾讯云 TDSQL：https://cloud.tencent.com/product/cdb

---

## 七、本地启动

### 准备

1. JDK 17+
2. Maven 3.6+
3. MySQL 8（或用 Docker）
4. 在 `application.yml` 填入第三方密钥，或设置环境变量

### 编译运行

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/commercehub.jar

# 或开发模式
mvn spring-boot:run
```

### 默认配置

- 服务端口：`8080`
- 接口前缀：`/api`
- 启动后访问：http://localhost:8080/api/

---

## 八、接口清单

所有接口统一前缀 `/api`，统一响应格式：

```json
{ "code": 0, "message": "OK", "data": {} }
```

### 支付 `/api/pay`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/pay/pay` | 发起支付（ALIPAY/WECHAT） |
| POST | `/pay/notify/alipay` | 支付宝异步回调 |
| POST | `/pay/notify/wechat` | 微信异步回调 |
| POST | `/pay/refund` | 退款 |
| GET  | `/pay/query/{outTradeNo}` | 主动查单 |

### 物流 `/api/logistics`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/logistics/create` | 电子面单下单 |
| GET  | `/logistics/trace/{trackingNo}` | 查物流轨迹 |
| GET  | `/logistics/order/{orderNo}` | 按订单号查运单 |

### 短信 `/api/sms`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sms/send` | 发送短信 |

### 实名认证 `/api/cert`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/cert/initialize` | 发起实名认证，返回认证链接 |
| GET  | `/cert/query/{bizNo}` | 查询认证结果 |
| GET  | `/cert/record/{userId}` | 查用户最近一次认证记录 |

---

## 数据表

共 5 张表，启动自动创建：

| 表 | 用途 |
|----|------|
| `pay_order` | 支付订单 |
| `pay_notify_log` | 支付回调日志（幂等） |
| `logistics_order` | 物流运单 |
| `sms_record` | 短信发送记录 |
| `cert_record` | 实名认证记录 |
