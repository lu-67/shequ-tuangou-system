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
| M0 用户与权限 | 注册登录（JWT+BCrypt）、三角色 RBAC | 🔧 开发中 |
| M1 商品管理 | 商品 CRUD + 动态条件查询 | 🔧 开发中 |
| M2 开团活动 | 开团 CRUD + 活动状态机（枚举 + 转移表 + 非法转移断言）、分页查询、进行中列表、角色校验 | ✅ 已完成（成员 B，19 个单测 + 7 个集成测试） |
| M4 支付与成团 | 支付幂等、回调 HMAC 验签防重入账、三重 CAS 幂等、退款回补库存 | ✅ 已完成（成员 B，18 个单测 + 5 个集成测试） |
| M5 自提点与核销 | 核销码幂等核销（同码仅一次成功） | 🔧 开发中 |
| M6 统计导出 | 订单查询、汇总统计、EasyExcel 导出 | 🔧 开发中 |
| M7 缓存与限流 | 库存预热、Cache-Aside、令牌桶限流 | 🔧 开发中 |
| M8 定时任务 | 到期判定 / 达标兜底 / 超时未支付自动取消 + 库存回补（可开关、幂等可重跑） | ✅ 已完成（成员 B，10 个单测） |

已实现接口（详见 `/doc.html`）：`GET /api/orders/{id}`（订单详情）、`GET /api/orders/my`（我的订单，需登录）、`POST /api/orders`（参团下单，需登录）；M2/M4/M8 的活动、支付、统计接口见 `/doc.html`（成员 B 提交）。

## 测试

```bash
cd community-groupbuy
mvn clean test    # 71 个用例：4 个 Service 单测类 + 3 个 MockMvc 集成测试类
```

覆盖：M3 参团下单全链路（活动不存在、活动非进行中、超过限购、库存不足 CAS 失败不落单、限流拦截、订单不存在、我的订单倒序）与 MockMvc 集成（200 / 400 / 超限购业务码 / 订单不存在 3001）；M2 活动状态机（合法流转 PENDING→ONGOING、非法转移拒绝、分页、角色校验）；M4 支付（重复支付拦截、重复回调幂等、验签失败拒绝、退款回补库存）；M8 定时任务（到期判定、超时取消并回补库存、开关控制、幂等可重复执行）。

## Git 协作

每人一个分支开发（如 `feat/m2-activity`），完成后提 Pull Request，组长 review 后合并。提交信息规范：`feat: xxx` / `fix: xxx` / `test: xxx` / `docs: xxx`。

## 说明

- 本项目为《Java开发综合实训》课程大作业，代码在开发过程中使用 AI 辅助生成，均已人工审查、按项目约定整改。
- 数据库连接、JWT 密钥等敏感配置走环境变量（见 `application-prod.yml`），仓库中不含任何真实密钥。
