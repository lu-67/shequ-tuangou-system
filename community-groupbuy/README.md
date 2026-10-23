# 社区团购（生鲜自提）拼团系统

一个面向社区的**生鲜团购拼团平台**（Spring Boot 3 + MySQL + Redis + Vue3 双前端）。居民可以浏览商品、参团下单、支付、到自提点核销取货；团长可以发起开团活动、管理自提点；管理员可以管理商品与活动、查看运营统计并导出。

**当前进度**：工程骨架与 **M3 参团下单（组长 A）** 已完成并可运行；M0/M1/M2/M4/M5/M6/M7/M8 由组员按分工开发中。

## Git 协作

- 远程仓库：`git clone` 后本地开发，**每人一个分支**（如 `feat/m2-activity`），完成后提 Pull Request 合并到 `main`，由组长 Review。
- 提交信息规范：`类型: 简述`，类型取 `feat` / `fix` / `test` / `docs` / `refactor`。
- 不要提交：`target/`、`node_modules/`、`dist/`、本地配置文件（已在 .gitignore）。

本工程分三部分：
- **后端**（本目录 `community-groupbuy/`）：Spring Boot 3 + MyBatis + MySQL + Redis 的 RESTful 服务，
  启动后可先用 Knife4j 接口文档（`/doc.html`）在线调试全部接口。
- **管理后台前端**（同级目录 `admin/`，端口 5173）：Vue3 + Vite + Element Plus 的管理后台（骨架状态，
  各模块页面由 C/D/B 同学实现），启动方式见 `admin/README.md`。
- **居民网页端**（同级目录 `web/`，端口 5174）：Vue3 + Vite + Element Plus 的 PC 网页端，
  含团购首页 / 我的订单 / 个人中心（登录页待 D 同学的 M0 落地），启动方式见 `web/README.md`。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 / 框架 | JDK 17 + Spring Boot 3.2（jakarta 命名空间） |
| 持久层 | MyBatis 3.5（手写动态 SQL）+ MySQL 8.0（utf8mb4 / InnoDB） |
| 缓存 | Redis 7（库存预热 + Cache-Aside + Lua 令牌桶限流） |
| 安全 | Spring Security + JWT（BCrypt）+ RBAC 角色权限 |
| 文档 | Knife4j（OpenAPI3，jakarta） |
| 导出 | EasyExcel |
| 前端（管理后台） | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios（见同级 `admin/`） |
| 前端（居民网页端） | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios（见同级 `web/`） |
| 测试 | JUnit 5 + Mockito + MockMvc（spring-security-test） |
| 构建 | Maven（`mvn clean package` 一次成功，测试随构建执行） |

## 目录结构

```
community-groupbuy/
├─ pom.xml                          # Maven 工程定义
├─ src/main/java/com/community/groupbuy/
│  ├─ CommunityGroupBuyApplication.java   # 启动类（含 DTD 放开、定时任务开关）
│  ├─ config/      # Security（当前 permitAll 占位）/ JwtUser / OpenAPI
│  ├─ common/      # 统一响应 Result / 全局异常 / 常量 / SecurityUtils
│  ├─ entity/      # GroupOrder / GroupActivity（其余实体由 C/B 同学按 schema 补建）
│  ├─ mapper/      # OrderMapper(+XML 动态 SQL) / ActivityMapper（M3 依赖的最小集）
│  ├─ dto/         # CreateOrderDTO / OrderQueryDTO（其余 DTO 由各模块负责人补建）
│  ├─ service/     # OrderService（M3 参团下单）/ RateLimitService（M3 依赖的限流组件）
│  └─ controller/  # OrderController（M3 参团下单）
├─ src/test/java/com/community/groupbuy/
│  ├─ service/OrderServiceTest.java       # M3 Service 单元测试（8 例，Mockito）
│  └─ controller/OrderControllerTest.java # M3 接口集成测试（4 例，MockMvc）
├─ src/main/resources/
│  ├─ application.yml            # 公共配置（含 spring.sql.init.mode=always）
│  ├─ application-dev.yml        # 开发环境（MySQL 3307 / Redis，密钥写死）
│  ├─ application-prod.yml       # 生产环境（密钥走环境变量）
│  ├─ db/schema.sql              # 建表（7 张关联业务表，utf8mb4/InnoDB，全组共用不改）
│  ├─ db/data.sql                # 演示数据（每主表≥15 条，订单 320 条，全组共用不改）
│  └─ mapper/OrderMapper.xml     # 手写动态 SQL
└─ target/                       # 编译产物（含可运行 jar）
```

> 仓库整体结构：同级目录 `admin/` 为管理后台前端（端口 5173）、`web/` 为居民网页端（端口 5174），
> 启动方式分别见各自的 README。数据库脚本（schema.sql / data.sql）为全组共用契约，**请勿修改**。

## 环境要求

- JDK 17 及以上（**运行必须用 JDK17+**；本机命令行默认 `java` 是 1.8 的 JRE，会报类版本错误，需用 JDK17+ 全路径）
- Maven 3.8+
- MySQL 8.0，端口 **3307**，账号 `root` / 密码 **123456**（连接信息见 `application-dev.yml`）
- Redis 7，端口 **6379**（**可选**：不启动 Redis 也能跑，核心流程自动降级到数据库）
- Node.js 16+ 与 npm 9+（**仅运行两个前端 `admin/`、`web/` 时需要**）
- 接口文档：启动后访问 `http://localhost:8080/doc.html`（Knife4j）

## 启动步骤

1. 创建数据库（仅需一次）：
   ```sql
   CREATE DATABASE community_groupbuy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
2. 在项目根目录执行（**需要 JDK 17+ 与 Maven 3.8+**；推荐用下面这条，无需写死 java 路径，换电脑也能直接用）：
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```
   > 提示：本机命令行默认的 `java` 可能是 1.8，直接 `java -jar` 会报类版本错误。用 `mvn spring-boot:run` 由 Maven 自身的 JDK 启动，
   > 不依赖系统 `java` 路径，换电脑也能通用。若坚持用 `java -jar`，只需把 `JAVA_HOME` 指向任意 JDK 17+ 即可，无需写死具体路径。
3. 启动后访问接口文档（Knife4j）：
   ```
   http://localhost:8080/doc.html
   ```

> 说明：项目启动时会通过 `spring.sql.init` **自动建表并写入演示数据**（每次启动重置）。
> 演示账号统一密码 `123456`：
> - 管理员：`admin`（可用于商品/活动/统计等需 ADMIN 权限的接口）
> - 团长：`head1` / `head2` / `head3`
> - 居民：`user05` ~ `user15`

## 当前进度与分工

| 模块 | 内容 | 负责人 | 状态 |
| --- | --- | --- | --- |
| 工程骨架（M9） | Maven 工程、统一响应 Result、全局异常、Constants、SecurityUtils、Knife4j | A（组长） | ✅ 已完成 |
| M3 参团下单 | Redis 预扣 + DB CAS 不超卖 + Lua 限流 + 跨表写事务 + 我的订单 | A（组长） | ✅ 已完成（含 12 个测试） |
| M0 用户与权限 | 注册/登录（JWT+BCrypt）、三角色 RBAC、SecurityConfig 中的 JWT 过滤链、两个前端的登录页 | D | ⬜ 待实现 |
| M1 商品管理 | 商品 CRUD + 动态条件查询（手写动态 SQL） | C | ⬜ 待实现 |
| M2 开团活动 | 开团 CRUD + 状态机（枚举+转移表+断言）+ 分布式锁防重复开团 | B | ⬜ 待实现 |
| M4 支付成团 | 支付幂等回调 + 成团判定 + 退款回补库存 | B | ⬜ 待实现 |
| M5 自提核销 | 核销码幂等核销（同码仅一次）+ 自提点列表 | C | ⬜ 待实现 |
| M6 统计导出 | 订单查询、汇总统计、EasyExcel 导出（≥300 行） | C / D | ⬜ 待实现 |
| M7 缓存与限流 | 库存预热、Cache-Aside、令牌桶限流扩展 | C | ⬜ 待实现（限流组件已随 M3 保留） |
| M8 定时任务 | 超时未支付自动取消 + 库存回补 | B | ⬜ 待实现 |

## 功能清单

**参团下单（M3 · 已实现）**
- 参团下单：限流 → 活动校验 → Redis 预扣（未预热/Redis 不可用时自动降级）→ DB CAS 原子扣库存 → 落订单（UNPAID）并生成核销码
- 整个扣库存与写订单在同一事务内，**保证不超卖**
- 我的订单（按当前登录用户，倒序）；订单详情查询
- 下单接口 Lua 令牌桶限流（Redis 不可用时放行，不阻断主流程）

**其余模块（M0/M1/M2/M4/M5/M6/M7/M8）**
- 由 B/C/D 同学按分工实现，各模块的验收点：
  - M2：状态机枚举 + 转移表 + 断言；分布式锁防止重复开团
  - M4：支付幂等（同一订单重复支付只生效一次）、成团判定、退款回补库存
  - M5：核销幂等（同一核销码连核多次仅成功一次）
  - M6：EasyExcel 导出 ≥300 行
  - M8：超时未支付自动取消并回补库存

**前端**
- `admin/`（5173）：管理后台骨架（侧边栏布局），各模块页面由 C/D/B 同学实现
- `web/`（5174）：居民网页端（顶部导航 + 卡片网格），团购首页 / 我的订单 / 个人中心；登录页待 M0 落地

## RESTful 接口一览

**当前已实现（M3）：**

| 模块 | 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- | --- |
| M3 下单 | POST | `/api/orders` | 参团下单（并发扣库存） | 登录 |
| M3 下单 | GET | `/api/orders/my` | 我的订单（居民网页端） | 登录 |
| M3 下单 | GET | `/api/orders/{id}` | 订单详情 | 登录 |

**各模块开发计划（合计 ≥12 个接口）：**

| 模块 | 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- | --- |
| M0 权限 | POST | `/api/auth/register` | 注册 | 公开 |
| M0 权限 | POST | `/api/auth/login` | 登录（返回 JWT） | 公开 |
| M1 商品 | GET | `/api/products` | 商品列表（动态条件） | 登录 |
| M1 商品 | GET | `/api/products/{id}` | 商品详情 | 登录 |
| M1 商品 | POST | `/api/products` | 新增商品 | ADMIN |
| M1 商品 | PUT | `/api/products/{id}` | 修改商品 | ADMIN |
| M1 商品 | DELETE | `/api/products/{id}` | 下架删除 | ADMIN |
| M2 开团 | GET | `/api/activities` | 活动列表（动态条件） | 登录 |
| M2 开团 | GET | `/api/activities/{id}` | 活动详情 | 登录 |
| M2 开团 | POST | `/api/activities` | 创建开团（分布式锁防重复） | ADMIN |
| M2 开团 | PUT | `/api/activities/{id}/status` | 状态机转移（断言） | ADMIN |
| M4 支付 | POST | `/api/payments/pay/{orderId}` | 支付（幂等）+ 成团判定 | 登录 |
| M4 支付 | POST | `/api/payments/refund/{orderId}` | 退款（回补库存） | 登录 |
| M5 核销 | POST | `/api/pickup/verify` | 自提核销（幂等，同码仅一次） | 登录 |
| M5 核销 | GET | `/api/pickup/points` | 自提点列表（居民网页端） | 登录 |
| M6 统计 | GET | `/api/stats/orders` | 订单查询 | ADMIN |
| M6 统计 | GET | `/api/stats/summary` | 汇总统计 | ADMIN |
| M6 统计 | GET | `/api/stats/export` | **Excel 导出（≥300 行）** | ADMIN |

## 测试（JUnit 5 + MockMvc）

```bash
mvn test        # 运行全部测试（随 mvn clean package 一起执行）
```

**已覆盖（M3，共 12 例）：**

| 测试类 | 类型 | 用例数 | 覆盖点 |
| --- | --- | --- | --- |
| `service/OrderServiceTest` | Service 单元测试（Mockito，不起容器） | 8 | 正常下单（校验金额/状态/核销码）、活动不存在、活动非进行中、超过限购、CAS 失败库存不足（不落订单）、限流拦截、订单不存在、我的订单倒序 |
| `controller/OrderControllerTest` | 接口集成测试（MockMvc + 真实容器 + MySQL） | 4 | 正常下单、非法参数返回 400、超过限购返回业务错误码、订单不存在返回 3001 |

测试要点（供 B/C/D 同学写自己模块的测试时参考）：
- **单元测试**：`@ExtendWith(MockitoExtension.class)` + `@Mock` Mapper + `@InjectMocks` Service；用 `assertThatThrownBy` 断言业务异常分支（老师要求 Service 单测 ≥6 例且含 ≥2 个异常分支）。
- **集成测试**：`@SpringBootTest(properties = "spring.sql.init.mode=never")`（不重置数据库，保护演示数据）+ `@AutoConfigureMockMvc` + `@Transactional`（用例回滚）；登录态用 `SecurityMockMvcRequestPostProcessors.user(new JwtUser(...))` 构造。
- **鉴权失败 / 越权用例**：待 D 同学的 M0（JWT 过滤链）落地后补充——不带 token 访问应返回 401，居民访问 ADMIN 接口应返回 403。
- 全组测试用例合计需 **≥12 例**、核心 Service 行覆盖 **≥50%**（可在 pom 中加 jacoco 插件生成覆盖率报告）。

## 关键技术点（对照验收）

- **并发不超卖（案例三验收）**：参团下单用 `UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?`
  （DB CAS，原子扣减，200 并发抢购 1000 库存成功单数恰为 1000）。`group_activity` 表 id=1 预置库存 1000 供压测。
- **缓存/限流**：库存预热到 Redis、Cache-Aside 回源、Lua 令牌桶限流（下单接口）；Redis 不可用时自动降级到数据库。
- **规范**：统一响应 `Result{code,message,data}`、全局异常（含参数校验 400 与请求体解析失败分支）、
  `jakarta.validation` 校验、分层 `controller/service/mapper/entity/dto/config/common`、SLF4J 日志、常量收敛至 `Constants`。
- **状态机 / 幂等 / 定时任务**：随 M2 / M4+M5 / M8 由 B/C 同学开发补充。

> 生产环境（prod）的数据库密码、Redis 密码、JWT 密钥请通过环境变量注入，切勿入库。
