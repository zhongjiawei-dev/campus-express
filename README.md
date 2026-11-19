# 校园快递代取系统

这是我做的一个校园快递代取 Web 项目。学生发布代取需求，代取员在接单大厅抢单，管理员处理实名审核、提现和投诉。后端用 Spring Boot 3 + MyBatis-Plus，前端用 Vue 2 + Element UI，默认使用 H2，也可以切到 MySQL。

- 后端：Spring Boot 3.2.5 / Java 17 / MyBatis-Plus 3.5.7 / JWT / H2（演示）或 MySQL 8（生产）
- Web 端：Vue 2.6 + Element UI 2.15（响应式 H5，手机和电脑浏览器都能用，无需 Node 构建）
- 管理后台：单页 HTML，Vue 3 + Element Plus（CDN）
- 测试：JUnit 5，7 个计价用例 + 6 个状态机用例

## 一、目录结构

```
campus-express/
├─ pom.xml
├─ start.ps1 / stop.ps1              # Windows 一键启停
├─ admin/
│  └─ index.html                     # 管理后台（直接用浏览器打开）
├─ web/                              # 学生 / 代取员 Web 端（Vue 2 + Element UI）
│  ├─ index.html                     # 单页入口，直接双击打开
│  ├─ css/app.css
│  └─ js/api.js / js/app.js
├─ sql/
│  └─ schema-mysql.sql                # MySQL 20 张表建库脚本
├─ docs/
│  └─ API.md                          # 接口清单与错误码
├─ src/main/resources/
│  ├─ application.yml                 # dev：H2；mysql profile：MySQL
│  └─ schema.sql                      # H2 演示库 14 张表
└─ src/main/java/com/campus/express/  # 后端源码
```

## 二、环境要求

| 组件 | 要求 | 本机现状 |
|---|---|---|
| JDK | 17+ | `C:\Program Files\Java\jdk-17`（17.0.12） |
| Maven | 3.9+ | 本机已内置 `tools/apache-maven-3.9.9`；从 GitHub 克隆后需自备 Maven（`start.ps1` 会自动查找 PATH 中的 `mvn`） |
| Node | 仅管理后台开发可选 | 已安装 |
| MySQL | 生产可选 | 已安装但 root 口令未知，默认走 H2 |
| 浏览器 | 运行 Web 端 | Chrome / Edge 均可 |

> 注意：本机系统环境变量 `JAVA_HOME` 指向了不存在的 JDK 1.8，直接执行 `mvn` 会报错。启动脚本已内置 `JAVA_HOME` 覆盖，手工执行命令时也必须先覆盖。

## 三、启动项目

在项目根目录执行：

```powershell
# 启动（默认 H2 内存库，重启后数据重置）
.\start.ps1

# 停止
.\stop.ps1
```

脚本会先设置 `JAVA_HOME`，再打包并启动服务。

启动成功后访问：

- 健康检查：`http://localhost:8080/api/v1/stations`（应返回 4 个站点）
- H2 控制台：`http://localhost:8080/h2-console`（JDBC URL `jdbc:h2:mem:campus`，用户名 `sa`，密码留空）

### 手工启动（等价命令）

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
Set-Location .\campus-express
mvn -DskipTests package
& "$env:JAVA_HOME\bin\java.exe" -jar "target\campus-express-1.0.0.jar"
```

首次 `mvn` 会下载依赖，约 1-2 分钟；之后打包约 15 秒。

## 四、演示账号

登录接口统一为 `POST /api/v1/auth/login`，演示环境用固定 code 模拟第三方登录凭证：

| 角色 | 登录参数 | 说明 |
|---|---|---|
| 学生 | `{"code":"student"}` | 林同学，钱包初始 100 元，1 条待接单、1 条已完成订单 |
| 代取员 | `{"code":"courier"}` | 周学长，已通过实名认证，可进接单大厅 |
| 管理员 | `POST /admin/login`，`admin` / `admin123` | 后台账号，密码 BCrypt 存储 |

Web 端打开后先在登录页选择身份；进入后可在"我的"页随时切换学生 / 代取员演示账号。切换逻辑在 `web/js/app.js` 的 `loginAs(code)`，接入真实登录时把 code 换成后端下发的登录凭证即可。

## 五、主要流程

1. 学生端首页：选快递类型 → 填站点、取件码、重量、预约时间、地址、备注/保价 → 查看报价 → 确认支付。
2. 支付走钱包余额（演示支付），订单进入"待接单"，可在"接单大厅"看到。
3. 切换代取员账号 → 接单大厅抢单 → 订单详情输入取件码 + 填写取件照片地址 → 确认取件。
4. 送达时填写送达照片地址 → 学生确认收货 → 平台按 90% 结算给代取员，10% 平台抽成。
5. 全程关键节点写入 `t_order_status_log`，并在"我的-消息"里生成站内通知。
6. 管理后台：看板、订单查询/平台取消、代取员实名审核、计价规则编辑、提现审核、投诉仲裁、公告群发。

演示待接单订单：`CE202611190000001`，取件码 `8821`。

## 六、Web 端运行

1. 先启动后端（`.\start.ps1`）。
2. 直接双击 `web/index.html`，或拖入 Chrome / Edge。
3. 登录页选择"学生"或"代取员"即可进入；同一浏览器可随时在"我的"页切换身份。
4. 若后端不在本机，把 `web/js/api.js` 第一行 `API_BASE` 改成电脑局域网 IP，例如 `http://192.168.1.10:8080/api/v1`，手机与电脑连同一 Wi-Fi 后用手机浏览器访问该页面。

如果浏览器因本地文件安全策略拦截请求，可在项目根目录起一个静态服务器：

```powershell
cd web
python -m http.server 5173
```

然后打开 `http://localhost:5173`。后端已配置 CORS，允许跨源调用。

页面依赖 unpkg 上的 Vue 2 与 Element UI，首次打开需要联网；离线答辩时可把两个 CDN 文件下载到 `web/vendor/`，再把 `web/index.html` 底部的引用改成本地路径。

## 七、管理后台运行

直接用浏览器打开 `admin/index.html`（双击即可），默认登录 `admin / admin123`。

后台通过 `fetch` 调用 `http://localhost:8080/api/v1`，因此需要后端先启动；页面依赖 unpkg 上的 Vue 3 与 Element Plus，首次打开需要联网。若在离线环境演示，可把两个 CDN 文件下载到 `admin/vendor/` 并改 `index.html` 的引用路径。

## 八、切换 MySQL（生产 / 答辩演示均可）

1. 用 root 执行建库脚本（会创建 `campus_express` 库与 20 张表）：

```powershell
& "<MySQL 安装目录>\bin\mysql.exe" -uroot -p -e "source sql/schema-mysql.sql"
```

2. 创建业务账号（脚本未创建账号，避免把口令写死）：

```sql
CREATE USER 'campus'@'localhost' IDENTIFIED BY 'campus123';
GRANT ALL PRIVILEGES ON campus_express.* TO 'campus'@'localhost';
FLUSH PRIVILEGES;
```

3. 以 mysql profile 启动：

```powershell
.\start.ps1 -Profile mysql
```

连接串、账号在 `application.yml` 的 `mysql` 文档块中，可用环境变量覆盖：

```powershell
$env:SPRING_DATASOURCE_USERNAME = "campus"
$env:SPRING_DATASOURCE_PASSWORD = "campus123"
.\start.ps1 -Profile mysql
```

首次连接空库时，`DataInitializer` 会自动写入管理员、计价规则与演示数据；正式上线前请清空演示订单，或移除该初始化器。

## 九、抢单并发锁切换 Redis

默认 `app.grab.mode=memory`，用 `ConcurrentHashMap` + 惰性过期实现单机原子占位，够单实例部署使用。

多实例部署时改为 Redis：

```powershell
$env:APP_GRAB_MODE = "redis"
$env:SPRING_DATA_REDIS_HOST = "127.0.0.1"
$env:SPRING_DATA_REDIS_PORT = "6379"
.\start.ps1
```

`RedisGrabLock` 用 Lua 脚本把 `SETNX + EXPIRE` 合成一个原子操作；抢单成功后写订单状态仍以数据库 CAS（`status=20 AND courier_id IS NULL`）为最终防线，缓存只做前置削峰。

## 十、测试

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\tools\apache-maven-3.9.9\bin\mvn.cmd test
```

- `FreightCalculatorTest`：3.80 / 15.20 / 10.80 三个算例，覆盖普通件、大件高峰、生鲜品类附加。
- `OrderStateMachineTest`：合法流转、非法跳转、取消限制。

## 十一、还没做的部分

| 能力 | 现状 | 上线前要补 |
|---|---|---|
| 支付 | 钱包余额扣款（Mock 支付流水） | 微信支付 / 支付宝 H5 下单、回调验签、金额比对 |
| 图片 | 前端填图片 URL | 对象存储直传 + 压缩 + 鉴黄 |
| 短信/订阅消息 | 站内消息表 | 短信通道、公众号模板消息 |
| 实名认证 | 掩码 + 人工审核 | 公安实名接口、证件 OCR |
| 敏感词 | 本地词库 | 接入第三方内容安全 |
| 提现 | 风控规则 + 人工审核 | 支付宝 / 微信商家转账、打款回调 |

## 十二、常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| 脚本无法运行，提示禁止执行 | PowerShell 执行策略 | `powershell -ExecutionPolicy Bypass -File .\start.ps1` |
| `mvn` 报 `JAVA_HOME` 不存在 | 系统变量指向 jdk1.8 | 先执行 `$env:JAVA_HOME="C:\Program Files\Java\jdk-17"` |
| 8080 被占用 | 旧进程未退出 | `.\stop.ps1` 后再启动 |
| Web 端请求失败 | 后端未启动、`API_BASE` 地址不对，或本地文件被浏览器拦截 | 先启动后端；改 `web/js/api.js`；或用 `python -m http.server` |
| 后台空白 | CDN 未加载 | 检查网络，或本地化 Element Plus 资源 |
| 控制台中文乱码 | Windows 终端编码 | 不影响功能，可 `chcp 65001` |
| MySQL 连接 1045 | root 口令未知 / 账号未建 | 按第八节建账号，或用 H2 演示 |
