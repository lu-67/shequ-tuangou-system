# 给 D 的注意事项（M0 用户权限 / M6 统计导出）

> 作者：成员 C（M1 商品 / M5 核销 / M7 缓存）
> 时间：2026-10-26
> 背景：C 的三个模块（PR #2 / #3 / #4）已全部合并进 `main`，组长评审通过。D 是最后一个模块，动 `SecurityConfig` 会影响全组接口，先把容易踩的坑对齐。

---

## 一、最容易出错的一条：角色取值是 `HEAD`，不是 `LEADER`

手册里 D 的提示词写的是 `RESIDENT（居民）/ LEADER（团长）/ ADMIN（管理员）`，
但**实际代码里统一是 `RESIDENT / HEAD / ADMIN`**：

| 来源 | 取值 |
|---|---|
| `db/schema.sql` 注释 | `RESIDENT/HEAD/ADMIN` |
| `common/Constants.java` | `ROLE_ADMIN / ROLE_HEAD / ROLE_RESIDENT` |
| B 的《给C和D的接口与协作说明》第 4 节 | 必须是 `ADMIN` / `HEAD` / `RESIDENT` |

**请一律使用 `HEAD`**。若用 `LEADER`，团长开团、商品管理、核销会全部被判越权（403）。

---

## 二、不要改这些文件/签名

| 不要动 | 原因 |
|---|---|
| `common/SecurityUtils` | C 和 B 共 10+ 处在用（`getUserId/getRole/getUsername`） |
| `config/JwtUser` 的构造签名 `(Long id, String username, String role, String password)` | 所有测试和业务都按这个签名构造 |
| `OrderController` | 组长 M3，内部已用 `SecurityUtils` 取登录人 |
| B 的 `ActivityService / PaymentService / SchedulerService` | B 的 M2/M4/M8 |
| C 的 `ProductService / PickupPointService / PickupVerificationService / CacheService / StockCacheService` | C 的 M1/M5/M7；如需加 `@PreAuthorize` 请先在群里说 |

你要做的只有：`JwtAuthFilter` + `DbUserDetailsService` + `SecurityConfig` 过滤器链 + `@PreAuthorize`。

---

## 三、把 `permitAll` 改回 `authenticated()` 时，务必放行这些路径

### 1. 认证与文档

- `/api/auth/login`、`/api/auth/register`
- `/doc.html`、`/swagger-ui/**`、`/swagger-ui.html`、`/v3/api-docs/**`、`/webjars/**`、`/knife4j/**`、`/favicon.ico`

### 2. 居民端首页依赖的只读接口（**重点，别漏**）

`web/src/views/Home.vue` 是**未登录状态**就要能看的，它一次性并发调三个接口：

```js
Promise.all([
  request.get('/activities/ongoing'),   // B 提供
  request.get('/products/list'),        // C 提供
  request.get('/pickup/points')         // C 提供
])
```

`Promise.all` 的特点：**任一失败整页空白**。所以这三个 GET 必须 `permitAll`（或前端先加登录页再访问首页）：

- `GET /api/activities/ongoing`
- `GET /api/products/list`
- `GET /api/pickup/points`

---

## 四、C 的接口权限约定（加 `@PreAuthorize` 时请对齐）

权限断言目前都在 **Service 层**（`assertAdminOrHead`），返回的是统一响应体 `{code:403, message:...}` + **HTTP 200**。

| 接口 | 要求 |
|---|---|
| `POST /api/products`、`PUT /api/products/{id}`、`PUT /api/products/{id}/status`、`DELETE /api/products/{id}` | ADMIN / HEAD |
| `POST /api/pickup/points`、`PUT /api/pickup/points/{id}`、`DELETE /api/pickup/points/{id}` | ADMIN / HEAD |
| `POST /api/pickup/verify` | ADMIN / HEAD |
| `GET /api/products`（分页）、`GET /api/products/list`、`GET /api/products/{id}` | 登录即可（`/list` 建议放行） |
| `GET /api/pickup/points`、`GET /api/pickup/points/{id}` | 登录即可（`/points` 建议放行） |

> 注意：Spring Security 的 401/403 是 **HTTP 状态码**，而业务异常是 **HTTP 200 + code**。两种混用会让前端拦截器行为不一致（`web/src/utils/request.js` 里 401 会跳登录页）。建议统一：**未登录 → HTTP 401；已登录但越权 → 业务 code 403**，并在测试里固化。

---

## 五、扩 `data.sql` 时的两个坑

### 1. 唯一索引不能撞

| 表 | 唯一键 |
|---|---|
| `group_order` | `uk_order_no`(order_no)、`uk_verification_code`(verification_code) |
| `pickup_verification` | `uk_order_id`(order_id)、`uk_code`(verification_code) |

现有演示数据：订单 `NO2026000001~`、核销码 `CG10000001~`（`CG + 8 位订单 ID`）。新增数据请继续用这个规则生成，避免主键冲突。

### 2. 要做核销演示，必须同时预置 PENDING 核销记录

核销的语义是（组长 review 时定的方案 A）：

```sql
UPDATE pickup_verification
SET status='DONE', pickup_point_id=?, verify_time=NOW()
WHERE order_id=? AND status='PENDING'
```

`data.sql` 里现有 **195 条 `PENDING`** 核销记录。如果你新增了大量 PAID 订单但没同步插 `pickup_verification`，核销会走 C 写的 INSERT 兜底路径——**功能不报错**，但和“预置 PENDING”的设计不一致，演示时解释起来会别扭。建议新数据照样一条订单配一条 PENDING。

另外 `spring.sql.init.mode=always`：**每次启动都会重置数据库**，别把本机联调数据当持久数据；改 `data.sql` 前群里说一声，避免打断别人正在跑的联调。

---

## 六、改完 `SecurityConfig` 必须跑一次全量 `mvn test`

现有 **6 个 MockMvc 集成测试类**（B 的 3 个 + C 的 3 个）都依赖当前的放行配置，以及“业务码 403 + HTTP 200”的断言风格：

- `ActivityControllerTest` / `PaymentControllerTest` / `OrderControllerTest`（B 与组长）
- `ProductControllerTest` / `PickupVerificationControllerTest`（C）

它们用 `SecurityMockMvcRequestPostProcessors.user(new JwtUser(...))` 注入登录态（不是真登录）。你的过滤器上线后如果这些用例变红，优先检查：

1. 是否把测试请求拦成 401（放行规则写漏）
2. 是否把业务 403 改成了 HTTP 403（断言不匹配）

---

## 七、前端两件事

1. **路由只追加、不整段替换**：`admin/src/router/index.js` 里已有 `/activities`（B）、`/products`、`/verification`（C）。你的 `/dashboard`（M6 统计）、`/orders` 追加到后面的注释位；`Layout.vue` 侧边栏同理加菜单项。
2. **登录后要写 `stores/user.js`**：`Layout.vue` 右上角显示的是 `user.userInfo?.nickname || username`，不写进去会一直显示“未登录”；`request.js` 拦截器还会读 `user.token` 拼 `Authorization: Bearer`，所以 token 也必须落到 store（并持久化，否则刷新即掉登录）。

---

## 八、节奏提醒

1. **先落 M0，别等 M6 做完再提 PR**。现在全组所有写接口都因为拿不到登录态返回 401（开团、商品增删改、核销都点不动），M0 一合并全组立刻能联调演示。
2. **提交节奏**：老师评分是“每人每周 ≥3 次有意义提交、有分支痕迹、commit message 可追溯”（4 分档，集中在末期只给 2 分）。目前 GitHub 上 qy500（C）15 条、lu-67（组长）若干、B 若干，**只有 D 还是 0 条**，尽快开始小步提交。
3. **分支流程**：你现在也建议走 collaborator 直推——`git checkout -b feat/m0-auth` → 小步提交 → `git push origin feat/m0-auth` → 提 PR 给组长 review。

---

## 九、M6 统计导出容易忽略的点

- 统计 SQL 走 **XML 手写动态 SQL**（项目要求“≥3 处动态 SQL”，C 的 `ProductMapper.xml`、B 的 `ActivityMapper.xml` 已各占一处）；
- EasyExcel 导出注意 `@ExcelProperty` 列名与前端约定一致，大数据量用 `EasyExcel.write(...).sheet().doWrite(data)` 分批写；
- `GET /api/stats/**` 记得只给 ADMIN / HEAD，居民不能看全平台销售额；
- 导出接口不要和分页接口共用 Redis 缓存 key，避免导出把缓存打穿。
