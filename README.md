# 社区团购（生鲜自提）拼团系统

面向社区的**生鲜团购拼团平台**。居民浏览商品、参团下单、支付后到自提点凭核销码取货；团长发起开团活动、管理自提点；管理员管理商品与活动、查看运营统计并导出。

Spring Boot 3 + MySQL + Redis 后端，Vue3 双前端（管理后台 + 居民网页端），前后端分离。

## 工程结构

| 目录 | 内容 | 端口 |
| --- | --- | --- |
| `community-groupbuy/` | 后端：Spring Boot 3.2 + MyBatis + MySQL + Redis + Spring Security + Knife4j | 8080 |
| `admin/` | 管理后台前端：Vue3 + Vite + Element Plus（侧边栏布局） | 5173 |
| `web/` | 居民网页端：Vue3 + Vite + Element Plus（PC 网页布局） | 5174 |

各部分详细说明见各自目录下的 `README.md`。

## 技术栈

- **后端**：JDK 17 · Spring Boot 3.2 · MyBatis · MySQL 8.0（utf8mb4/InnoDB）· Redis 7 · Spring Security + JWT · Knife4j · EasyExcel · Maven
- **前端**：Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · Axios
- **测试**：JUnit 5 · Mockito · MockMvc

## 快速开始

```bash
# 1. 后端（需本地 MySQL 3307 / root / 123456，Redis 可选——未启动时自动降级不报错）
cd community-groupbuy
mvn clean package          # 编译 + 跑测试（12 个用例）
mvn spring-boot:run        # 启动，访问 http://localhost:8080/doc.html 在线调试接口

# 2. 管理后台
cd admin && npm install && npm run dev        # http://localhost:5173

# 3. 居民网页端
cd web && npm install && npm run dev          # http://localhost:5174
```

数据库脚本（`community-groupbuy/src/main/resources/db/`）：`schema.sql` 建 9 张表，`data.sql` 灌演示数据，**每次启动自动重置**。两个前端通过 Vite 代理把 `/api` 转发到 8080，无需配 CORS。

## 功能与进度

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| 工程骨架 | 统一响应 Result / 全局异常 / 参数校验 / dev-prod 配置分离 / Knife4j / 数据库脚本 | ✅ 已完成（组长） |
| M3 参团下单 | Lua 令牌桶限流 → Redis 库存预扣（降级）→ DB CAS 防超卖 → 同事务落订单 + 核销码生成 → 我的订单/详情 | ✅ 已完成（组长，含 12 个测试） |
| M0 用户与权限 | Spring Security + JWT + BCrypt 注册登录、三角色 RBAC（ADMIN/HEAD/RESIDENT）、JWT 过滤器鉴权、管理后台与居民端双登录页 | ✅ 已完成（成员 D） |
| M1 商品管理 | 商品 CRUD + 上下架 + 分页动态查询（手写动态 SQL）、商品被活动引用时禁止删除 | ✅ 已完成（成员 C，14 个单测 + 7 个集成测试） |
| M2 开团活动 | 开团 CRUD + 活动状态机（枚举 + 转移表 + 非法转移断言）、分页查询、进行中列表、角色校验 | ✅ 已完成（成员 B，19 个单测 + 7 个集成测试） |
| M4 支付与成团 | 支付幂等、回调 HMAC 验签防重入账、三重 CAS 幂等、退款回补库存 | ✅ 已完成（成员 B，18 个单测 + 5 个集成测试） |
| M5 自提点与核销 | 自提点 CRUD + 核销记录引用校验；核销码幂等核销（同一码连核 5 次仅第 1 次成功，其余返回 4002） | ✅ 已完成（成员 C，10 个单测 + 6 个集成测试） |
| M6 统计导出 | 运营大盘（概览统计）、订单数据 EasyExcel 导出（XML 动态 SQL）、统计接口按 ADMIN/HEAD 授权 | ✅ 已完成（成员 D） |
| M7 缓存与限流 | Cache-Aside + SETNX 分布式锁回源 + 双检 + Lua 释放锁 + 随机 TTL 防雪崩（全程 fail-open）；库存预热（key 与 M3 预扣一致）；商品详情 60s / 活动详情 20s 缓存并在状态流转时主动失效 | ✅ 已完成（成员 C，15 个缓存相关测试，含 100 并发回源验收） |
| M8 定时任务 | 到期判定 / 达标兜底 / 超时未支付自动取消 + 库存回补（可开关、幂等可重跑） | ✅ 已完成（成员 B，10 个单测） |

已实现接口（详见 `/doc.html`）：`POST /api/auth/login`、`POST /api/auth/register`（登录注册）、`GET /api/orders/{id}`（订单详情）、`GET /api/orders/my`（我的订单，需登录）、`POST /api/orders`（参团下单，需登录）；活动、支付、商品、自提点、统计导出接口见 `/doc.html`（成员 B/C/D 提交）。

> 除登录注册与居民端只读接口（`/api/activities/ongoing`、`/api/products/list`、`/api/pickup/points`）外，其余接口均需携带 `Authorization: Bearer <token>`。

## 测试

```bash
cd community-groupbuy
mvn clean test    # 133 个用例：10 个 Service 单测类 + 5 个 MockMvc 集成测试类（需本机 MySQL 3307 + Redis 6379）
```

覆盖：M3 参团下单全链路（活动不存在、活动非进行中、超过限购、库存不足 CAS 失败不落单、限流拦截、订单不存在、我的订单倒序）与 MockMvc 集成（200 / 400 / 超限购业务码 / 订单不存在 3001）；M2 活动状态机（合法流转 PENDING→ONGOING、非法转移拒绝、分页、角色校验）；M4 支付（重复支付拦截、重复回调幂等、验签失败拒绝、退款回补库存）；M8 定时任务（到期判定、超时取消并回补库存、开关控制、幂等可重复执行）；M1 商品（分页动态查询、团购价校验、状态流转、商品被活动引用禁止删除）；M5 自提点与核销（CRUD、已被核销记录引用禁止删除、**同一核销码连核 5 次仅第 1 次成功**）；M7 缓存（Cache-Aside 命中/未命中/锁竞争、**热点 key 过期瞬间 100 并发回源实测 1 次**、预热 key 100 并发回源 0 次）。

## Git 协作

每人一个分支开发（如 `feat/m2-activity`），完成后提 Pull Request，组长 review 后合并。提交信息规范：`feat: xxx` / `fix: xxx` / `test: xxx` / `docs: xxx`。

## 说明

- 本项目为《Java开发综合实训》课程大作业，代码在开发过程中使用 AI 辅助生成，均已人工审查、按项目约定整改。
- 数据库连接、JWT 密钥等敏感配置走环境变量（见 `application-prod.yml`），仓库中不含任何真实密钥。

## 已知问题与版本说明

本节统一更正上文在开发过程中留下的阶段信息，最终以本节和 `community-groupbuy/README.md` 为准。

- **测试用例数量**：上文「快速开始」「测试」两处写的 12 个、133 个用例是阶段性数字；最终 `mvn clean package` 执行 **159 个用例**（19 个测试类），JaCoCo 行覆盖率 85.9%、核心 Service 层 90.6%，明细见 `测试材料/覆盖率报告/` 与 `测试材料/测试用例汇总_规范版.xlsx`。
- **数据表数量**：上文「数据库脚本」处的「9 张表」不准确，`db/schema.sql` 实际建 **7 张业务表**（sys_user、product、group_activity、group_order、pickup_point、payment、pickup_verification）。
- **前端联调方式**：dev 环境两个前端通过各自 `.env.development` 的 `VITE_API_BASE` 直连 `http://127.0.0.1:8080/api`，后端 `SecurityConfig` 配了 CORS 白名单（仅放行 localhost / 127.0.0.1 的 5173、5174）；prod 不配该变量时回退同源 `/api`，由 Nginx 转发。上文「Vite 代理 `/api`、无需配 CORS」是初版方案，已不再使用。
- **Redis 未启动**：分布式锁、库存预扣、令牌桶限流全部 fail-open 自动降级，核心流程不中断；但热点回源、限流等压测类用例需要 Redis 6379 才能测出真实结果。
- **每次启动重置数据**：`spring.sql.init.mode=always`，启动时先 DROP 再重建表并重新灌入 `data.sql`，联调产生的数据重启即清空，演示过程中请勿重启后端。
- **新环境一键运行**：MySQL 3307 上若还没有 `community_groupbuy` 库，连接串已带 `createDatabaseIfNotExist=true` 会自动建库；随后 `schema.sql` 与 `data.sql` 由 Spring 启动时自动执行，无需手工导入（也可先手工执行 `CREATE DATABASE community_groupbuy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;`）。演示账号统一密码 `123456`：管理员 `admin`、团长 `head1`~`head3`、居民 `user05`~`user15`。
- **协作与提交记录**：本组使用 GitHub（https://github.com/lu-67/shequ-tuangou-system）配合微信群协作，`main` + `feature/xxx` 分支、Pull Request 合并；提交记录导出见 `测试材料/提交记录/git-log.txt`，截图与代码归属说明见《项目报告书》第 6.1 节，微信讨论原始截图见 `协作材料/`。

## 已知问题（运行时）

| 现象 | 原因 | 处理方式 |
| --- | --- | --- |
| 登录页报「网络错误，请确认后端已启动」 | 本机系统代理接管了页面内 `/api` 请求 | dev 已改为直连 127.0.0.1:8080；若仍失败，把 127.0.0.1 加入代理绕过列表或临时关代理 |
| 后端启动报端口 51696 被占用 | WorkBuddy 注入的 `SERVER__PORT` 被 Spring 读成 `server.port` | 启动前 `unset SERVER__PORT` 再执行 `mvn spring-boot:run` |
| `spring-boot:repackage` 打包失败 | 上一次运行的 java 进程还锁着 jar | 先结束旧 java 进程再重新打包 |
| 删过源码后启动报「找不到旧类」 | `target/classes` 残留旧 class | 先执行 `mvn clean` |
| Word 打开报告书目录页码为 0 | 目录是 Word 域 | 打开后右键「更新域」刷新页码 |
