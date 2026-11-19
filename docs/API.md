# 接口文档（v1）

基础地址：`http://localhost:8080/api/v1`
管理后台基础地址：`http://localhost:8080/api/v1/admin`

## 一、通用约定

### 1. 响应结构

```json
{ "code": 0, "message": "success", "data": {}, "traceId": "..." }
```

- `code=0` 表示成功；非 0 时 `message` 为可直接展示的中文提示。
- `traceId` 由全局异常处理器生成，便于日志排查。

### 2. 鉴权

除登录、站点列表外，其余接口需带：

```
Authorization: Bearer <token>
```

角色由 JWT 中的 `role` 决定：`USER` / `COURIER` / `ADMIN`。管理员接口同时校验角色。

### 3. 分页

请求 `page`（从 1 开始）、`size`（默认 10），响应为 MyBatis-Plus 分页对象：

```json
{ "records": [], "total": 12, "size": 10, "current": 1, "pages": 2 }
```

### 4. 时间格式

请求与响应均为 `yyyy-MM-dd HH:mm:ss`（Jackson 已统一配置，入参兼容 ISO 的 `T` 分隔）。

## 二、认证

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | Web 端登录，入参 `{"code":"student","nickname":"林同学"}` |
| GET | `/auth/me` | 当前登录用户信息 |

演示 code：`student`（学生）、`courier`（已认证代取员）。生产环境替换为账号密码、学校统一身份认证等真实登录凭证，由后端完成校验。

## 三、站点与地址

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/stations` | 站点列表 |
| GET | `/stations/{id}` | 站点详情 |
| GET | `/addresses` | 我的地址（手机号、房间号掩码，详细地址解密后返回） |
| POST | `/addresses` | 新增地址 |
| PUT | `/addresses/{id}` | 修改地址 |
| DELETE | `/addresses/{id}` | 删除地址 |
| POST | `/addresses/{id}/default` | 设为默认 |

新增地址入参：

```json
{
  "contactName": "林同学", "phone": "13800138000", "campus": "东校区",
  "building": "8 号楼", "room": "302", "detail": "门口有快递架",
  "lat": 30.2752, "lng": 120.1532, "isDefault": true
}
```

`detail` 落库前 AES-256-GCM 加密，`phone` 仅存掩码。

## 四、计价

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/price/quote` | 试算运费，不落单 |

```json
{
  "stationId": 1, "orderType": 2, "weightGram": 8000,
  "addressId": 1, "expectTime": "2026-11-19T12:00:00",
  "insured": true, "declaredValue": 1000
}
```

响应字段：`baseFee`、`weightFee`、`distanceFee`、`peakFee`、`typeExtraFee`、`insureFee`、`discountAmount`、`totalFee`、`distanceM`、`ruleNo`、`peak`。

## 五、订单

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| POST | `/orders` | USER | 创建订单 |
| GET | `/orders?group=all\|active\|done&page=&size=` | 登录用户 | 我的订单 |
| GET | `/orders/hall?sort=default\|price_desc\|price_asc\|distance_asc` | COURIER | 接单大厅 |
| GET | `/orders/{orderNo}` | 登录用户 | 详情（含地址解密、状态流水） |
| POST | `/orders/{orderNo}/pay` | USER | 钱包支付 |
| POST | `/orders/{orderNo}/cancel` | USER/ADMIN | 取消，入参 `{"reason":"..."}` |
| POST | `/orders/{orderNo}/confirm` | USER | 确认收货并结算 |
| POST | `/orders/{orderNo}/grab` | COURIER | 抢单 |
| POST | `/orders/{orderNo}/pick` | COURIER | 取件核验，入参 `{"pickupCode":"8821","photoUrl":"..."}` |
| POST | `/orders/{orderNo}/deliver` | COURIER | 送达，入参 `{"photoUrl":"..."}` |
| POST | `/orders/{orderNo}/appeal` | USER | 发起售后，入参 `{"type":1,"description":"...","demand":"...","images":"..."}` |

创建订单入参：

```json
{
  "stationId": 1, "orderType": 1, "weightGram": 2000, "pickupCode": "8821",
  "addressId": 1, "expectStart": "2026-11-19T18:00:00", "expectEnd": "2026-11-19T20:00:00",
  "remark": "放门口", "insured": false, "declaredValue": null,
  "discount": 0, "payType": 1, "urgent": false
}
```

响应：`orderNo`、`status`、`totalFee`、`distanceM`、`peak`、`price`（计价明细）。

订单详情响应：

```json
{
  "order": { "orderNo": "...", "status": 40, "statusText": "已取件", "pickupCodeMask": "**21", "...": "..." },
  "address": { "contactName": "...", "phoneMask": "...", "building": "...", "room": "...", "detail": "..." },
  "stationName": "菜鸟驿站·东区店",
  "courierName": "周明",
  "statusText": "已取件",
  "logs": [ { "fromStatus": 30, "toStatus": 40, "event": "PICK", "createdAt": "..." } ]
}
```

敏感字段 `pickupCode`（密文）与 `addressSnapshot`（密文）已标注 `@JsonIgnore`，不会出现在响应中。

## 六、钱包与提现

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/wallet` | 余额、累计收入/支出 |
| GET | `/wallet/flows?page=&size=` | 钱包流水 |
| GET | `/wallet/limits` | 当前提现规则与当日/当月剩余额度 |
| POST | `/wallet/withdraw` | 申请提现，入参 `{"amount":20,"channel":1,"account":"wx_openid"}` |
| GET | `/wallet/withdraws` | 我的提现记录 |

提现风控：单次 ≥ 10 元、单日 ≤ 500 元、单月 ≤ 2000 元。申请时先扣减余额并写支出流水，审核通过确认打款；驳回则原路退回余额。

## 七、代取员

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/courier/apply` | 提交实名认证 |
| GET | `/courier/profile` | 档案 + 钱包 + 今日收入 + 进行中单量 |
| GET | `/courier/me` | 仅档案 |
| PUT | `/courier/settings` | 服务时段、可接重量、服务站点、接单开关 |
| GET | `/courier/orders?status=&page=&size=` | 我的配送单 |
| GET | `/courier/income?page=&size=` | 收入流水 |

认证入参：

```json
{
  "realName": "林同学", "studentNo": "202303704", "idCard": "330203200401011234",
  "studentCardUrl": "https://example.com/card.jpg",
  "serviceStart": "08:00", "serviceEnd": "21:00",
  "maxWeightGram": 8000, "stationIds": "1,2"
}
```

身份证仅存掩码；审核状态 `1` 待审、`2` 通过、`3` 驳回。

## 八、消息

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/messages?page=&size=` | 消息列表 |
| GET | `/messages/unread-count` | 未读数 |
| POST | `/messages/{id}/read` | 标记已读 |

订单节点（支付、接单、取件、送达、完成、退款、审核）自动写入站内消息。

## 九、订单评价

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/evaluations` | 学生评价已完成订单，入参 `{"orderNo":"...","score":5,"tags":"准时送达,态度好","content":"...","anonymous":false}` |
| GET | `/evaluations/{orderNo}` | 查询该订单的评价，未评价返回 `data=null` |

- 仅订单所属学生可评价，且订单状态必须为 `60 已完成`；每单只能评价一次。
- `score` 为 1–5 星；`tags` 逗号分隔；`content` 入库前经过敏感词过滤。

## 十、管理后台

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/login` | `{"username":"admin","password":"admin123"}` |
| GET | `/admin/dashboard` | 今日单量/交易额、7 日 GMV、活跃用户、状态分布、绩效 Top、待办数 |
| GET | `/admin/orders?status=&keyword=&page=&size=` | 订单分页 |
| POST | `/admin/orders/{orderNo}/cancel` | 平台取消 |
| GET | `/admin/users?page=&size=` | 用户列表（掩码） |
| GET | `/admin/couriers?auditStatus=&page=&size=` | 代取员列表 |
| POST | `/admin/couriers/{id}/audit` | 实名审核，`{"pass":true,"remark":"..."}` |
| GET | `/admin/price-rules` | 生效中的计价规则 |
| POST | `/admin/price-rules` | 新增/更新规则（带 `id` 为更新，版本号自增） |
| GET | `/admin/withdraws?status=&page=&size=` | 提现申请 |
| POST | `/admin/withdraws/{id}/audit` | 提现审核 |
| GET | `/admin/complaints?status=&page=&size=` | 投诉列表 |
| POST | `/admin/complaints/{id}/resolve` | 仲裁，`{"result":"...","compensation":20,"responsibility":2}` |
| POST | `/admin/notices` | 公告群发，`{"title":"...","content":"..."}` |

`responsibility`：`1` 平台、`2` 代取员、`3` 用户；判定代取员责任时同时累加违规次数。

## 十一、错误码

| 码 | 含义 |
|---|---|
| 0 | 成功 |
| 10001 | 参数错误 |
| 10002 | 未登录或登录过期 |
| 10003 | 无权限 |
| 10004 | 请求过于频繁 |
| 20001 / 20002 | 用户不存在 / 账号禁用 |
| 20003 | 未完成代取员认证 |
| 20004 | 余额不足 |
| 30001 | 订单不存在 |
| 30002 | 订单状态不允许该操作 |
| 30003 | 状态已变化，请刷新重试 |
| 30004 | 订单已被接单 |
| 30005 | 订单已超时关闭 |
| 30006 | 取件码错误 |
| 30007 | 超出接单能力 |
| 40001 ~ 40004 | 支付失败 / 验签失败 / 金额不一致 / 重复支付 |
| 50001 ~ 50003 | 低于最低提现 / 超出限额 / 实名不一致 |
| 60001 | 内容含敏感词 |
| 90001 | 系统异常 |

## 十二、调用示例

```bash
# 学生登录
curl -X POST http://localhost:8080/api/v1/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"code\":\"student\"}"

# 报价
curl -X POST http://localhost:8080/api/v1/price/quote ^
  -H "Content-Type: application/json" -H "Authorization: Bearer <token>" ^
  -d "{\"stationId\":1,\"orderType\":1,\"weightGram\":2000,\"addressId\":1}"

# 抢单
curl -X POST http://localhost:8080/api/v1/orders/CE202611190000001/grab ^
  -H "Authorization: Bearer <courier-token>"
```
