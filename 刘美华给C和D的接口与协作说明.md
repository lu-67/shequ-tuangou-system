# 给 C、D 的接口与协作说明（避免合并冲突）

> 作者：成员 B（M2 开团活动 / M4 支付与成团 / M8 定时任务）
> 时间：2026-10-26
> 原则：**先说清我占了什么、留给你们什么、哪些地方必须统一下**，大家照着写代码，合并时就不会打架。

---

## 零、当前状态（先对齐事实）

| 项 | 状态 |
|---|---|
| B 的三个模块 | ✅ 已完成，71 个测试全绿，200 次并发回调压测通过 |
| 代码位置 | GitHub：`shequ-tuangou-system` 仓库 `feat/m2-activity` 分支 |
| 合并申请 | **PR #1 已提交**（9 条提交 / 31 个文件），等待组长 review 合并 |
| 我动了哪些表 | 只用不改：`group_activity`、`group_order`、`payment` |
| 我没碰什么 | `product`、`pickup_point`、`pickup_verification`、`sys_user` —— **一行都没写** |

**重要：C、D 请等组长合并完我的 PR #1 之后，再从最新 main 拉自己的分支**（具体流程见第七节）。否则共用文件必然冲突。

---

## 一、我占用的 31 个文件（请勿重复修改）

| 类别 | 文件 | 说明 |
|---|---|---|
| M2 活动 | `ActivityController / ActivityService / ActivityStatus / ActivityMapper(.xml) / CreateActivityDTO / ActivityQueryDTO` | 活动模块全部 |
| M4 支付 | `PaymentController / PaymentService / Payment 实体 / PaymentMapper(.xml) / PaySignUtils / CreatePaymentDTO / PaymentCallbackDTO / PaymentQueryDTO` | 支付模块全部 |
| M8 定时 | `SchedulerService` | 三个定时任务 |
| 测试 | `ActivityServiceTest / PaymentServiceTest / SchedulerServiceTest / ActivityControllerTest / PaymentControllerTest` | 5 个测试类 |
| admin 前端 | `admin/src/views/Activity.vue`（新增）、`router/index.js`、`layout/Layout.vue`、`utils/request.js` | 开团活动页面 + 路由/菜单/请求兼容 |
| 共用（仅追加） | `Constants.java`、`ResultCode.java`、`OrderMapper.java` | 见下一节，**追加方式，不动原有内容** |

> 上面这些文件在 PR #1 合并前，麻烦大家**不要同时改**；确需改动请在群里说一声，由我这边配合。

---

## 二、四个「合并热点」文件（最容易冲突，务必注意）

### 1. `common/Constants.java` —— 我只在末尾追加了 2 个常量

```java
public static final String PAY_CHANNEL_WECHAT = "WECHAT"; // 默认支付渠道
public static final String PAY_NO_PREFIX = "PAY";         // 支付流水号前缀
```

**给 C 的提醒**：Redis 相关常量组长**已经写好了**，直接复用，**不要重复定义**：

```java
Constants.REDIS_STOCK_PREFIX      // "groupbuy:stock:activity:"   库存预热
Constants.REDIS_RATE_LIMIT_PREFIX // "groupbuy:ratelimit:order:"  令牌桶限流
Constants.RATE_LIMIT_CAPACITY     // 200
```

### 2. `common/ResultCode.java` —— 我只追加了 3 个枚举值

```java
ACTIVITY_STATUS_ERROR(2004, "活动状态流转非法"),
PAY_SIGN_ERROR(3004, "支付回调验签失败"),
PAY_AMOUNT_MISMATCH(3005, "支付金额与订单金额不一致"),
```

**给 C、D**：你们的新响应码请**追加在自己业务号段末尾**（C：1000 商品段/4000 核销段；D：1000 用户段/6000 统计段），**不要改行号顺序**，这是最容易产生冲突的地方。

### 3. `mapper/OrderMapper.java` —— 我只在末尾追加了方法，组长原有的 `insert / selectById / updatePayStatus` 一个字没动

追加的是这 4 个：

```java
selectByOrderNo(String orderNo)                    // M4 回调对账：按订单号查订单
updateStatusCAS(id, from, to)                      // M4 订单状态 CAS 流转（支付入账自动写 pay_time）
selectTimeoutUnpaid(LocalDateTime before)          // M8 超时未支付订单
sumPaidQuantity(Long activityId)                   // 成团判定：已支付份数合计
```

**给 C、D**：如果你们也要用订单表，直接调用即可，**别再写一个同名方法**。

### 4. `application-dev.yml` —— 我只在 `app:` 节点下追加了配置

```yaml
app:
  pay:
    secret: community-groupbuy-pay-dev-secret-2026   # M4 回调验签密钥（演示环境）
    timeout-minutes: 15                              # M8 超时未支付自动取消
  scheduler:
    enabled: true                                    # M8 定时任务总开关
```

**给 C**：Redis 配置请加在其他位置，**不要覆盖这 3 行**。本地调试可用 `APP_SCHEDULER_ENABLED=false` 关掉我的定时任务（否则样例数据里超时订单会被自动取消，影响联调）。

### 5. admin 前端三件套（已改）

- `router/index.js`：我在 `children` 数组里注册了 `/activities` 路由 → **C、D 请追加在自己路由注释标记处，不要整段替换**
- `layout/Layout.vue`：侧边栏已改成真菜单（深蓝），我加了「开团活动」项，其余模块菜单留了注释位置
- `utils/request.js`：加了「接口失败静默降级」支持（避免某个模块没实现导致整页空白），传 `{ silent: true }` 即可

---

## 三、数据库表归属（约定：不改表结构）

| 表 | 负责人 | B 的使用方式 |
|---|---|---|
| `sys_user` | **D**（M0） | 我只通过 `SecurityUtils` 读 id / username / role |
| `product` | **C**（M1） | 我没碰。活动表通过 `product_id` 关联商品 |
| `group_activity` | **B**（M2） | 状态机 + 库存 CAS |
| `group_order` | 组长（M3）+ B（M4） | B 只做状态流转与已支付份数汇总 |
| `pickup_point` | **C**（M5） | 我没碰 |
| `pickup_verification` | **C**（M5） | 我没碰 |
| `payment` | **B**（M4） | 支付流水表，B 全权负责 |

> 我没有新增或删除任何字段。如果 C、D 觉得必须加字段，**先找组长**（他有"不改表结构"的约定）。

---

## 四、给 D 的 M0 说明（我最依赖你）

### 我的接口现在全都是 401，原因就在 M0 未落地

我的控制器里统一通过 `SecurityUtils` 取登录人：

```java
Long userId = SecurityUtils.getUserId();     // 报错：UNAUTHORIZED
String role = SecurityUtils.getRole();
```

调用点一共 7 处：`ActivityController`（创建活动、变更状态）、`PaymentController`（发起支付、退款）、以及组长 `OrderController` 里 2 处。

### 我需要你做成这样（其余你自由发挥）

1. JWT 过滤器解析 `Authorization: Bearer <token>`，把 **`JwtUser` 对象放进 `SecurityContext`**（类已存在，`config/JwtUser.java`）
2. **不要改 `JwtUser` 的构造签名**，它现在是 `(Long id, String username, String role, String password)`
3. **不要改 `SecurityUtils`**，我全靠它
4. `role` 的取值必须是这三个之一（`Constants` 里已定义）：`ADMIN` / `HEAD` / `RESIDENT`
5. 密码用 BCrypt（`SecurityConfig` 里组长留了 BCrypt 相关配置）

### 我的权限规则（你实现鉴权时可以直接用）

| 接口 | 要求 |
|---|---|
| 创建活动、变更活动状态 | **仅 ADMIN / HEAD** |
| 发起支付、退款 | 本人订单 或 ADMIN |
| 查询类（活动列表/详情/进行中、支付列表） | 登录即可 |

### 顺手一个提示

我写 MockMvc 测试时用 `user(new JwtUser(1L, "admin", "ADMIN", ""))` 注入登录态绕过登录，你写路由守卫测试可以照抄这个套路，不用真的去登录。

---

## 五、给 C 的 M1 / M5 / M7 说明

### 1. 两个前端页面在等你的接口（现在返回 500）

| 接口 | 页面 | 现状 |
|---|---|---|
| `GET /api/products` | 居民端首页、管理后台开团弹窗商品下拉 | ❌ 未实现，下拉框显示 No data |
| `GET /api/pickup/points` | 居民端首页、我的订单 | ❌ 未实现 |

> 提示：因为首页用的是"三个请求全成功才渲染"，你任一接口挂了整页就空白。我在本地做了容错（未提交），你可以先只实现接口，前端兼容让组长定。

### 2. M7 Redis 库存预热的挂载点（手册里点名要放在活动状态流转处）

按手册要求，"活动创建 / 状态变为 ONGOING 时把 stock 写入 Redis"。我已经梳理好插入位置，**你直接在 `ActivityService` 里加调用即可，不要重写整个方法**（避免跟我冲突）：

```java
createActivity(...)      // 创建活动成功后 → 若初始状态为 ONGOING，写缓存
updateStatus(...)        // 流转到 "ONGOING" 之后 → 写缓存；流转到 SUCCESS/FAILED → 删 key
markSuccessIfReached(...)// 成团时 → 清缓存
```

扣减走的是组长原有的 `ActivityMapper.deductStockCas`（注意是单数 `Cas`，不是 `CAS`），缓存与数据库的一致性由你决定策略。

**建议**：如果 M7 需要一个独立的 Service，你在外部写好，在 `ActivityService` 里注入调用一行即可 —— 这样我的 PR 合并后你改 3 行就能接上，冲突最小。

### 3. 令牌桶限流

组长已写了 `RateLimitService`，常量 `RATE_LIMIT_CAPACITY=200`，`OrderController` 已在用。你如果改造，注意别影响下单链路。

---

## 六、接口契约：我提供的接口 + 一个需要统一的冲突

### 我提供的接口（合并后即可用，共 11 个）

**活动（M2）**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/activities` | 创建活动（ADMIN/HEAD） |
| PUT | `/api/activities/{id}/status?status=ONGOING` | 状态流转（注意：**status 走 URL 参数，不是 JSON body**） |
| GET | `/api/activities` | 分页 + 动态条件查询 |
| GET | `/api/activities/ongoing` | 进行中活动列表（**返回数组**，首页用） |
| GET | `/api/activities/{id}` | 活动详情 |

**支付（M4）**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/payments` | 发起支付，body：`{orderId, channel}`，幂等 |
| POST | `/api/payments/callback` | 支付回调（HMAC-SHA256 验签 + 幂等） |
| POST | `/api/payments/{id}/refund` | 退款：订单置 CANCELLED + 回补库存 |
| GET | `/api/payments/{id}` | 支付单详情 |
| GET | `/api/payments` | 支付记录分页列表 |
| GET | `/api/payments/sign-demo` | 演示用：生成回调签名，方便自测（生产应移除） |

### ⚠️ 必须统一的两处格式

**① 分页返回值是对象，不是数组！**

```json
{ "code":200, "message":"...", "data": { "total": 15, "list": [...] } }
```

`GET /api/activities` 和 `GET /api/payments` 都是这个结构。前端取值请写 `res.data.list`，不要直接把 `data` 当数组遍历（我已经在 admin 活动页做过兼容，新页面请直接用正确写法）。

**② 居民端「在线支付」按钮调的路径不存在（契约冲突）**

`web/src/views/Orders.vue` 第 131 行调用的是：

```
POST /payments/pay/{orderId}
```

而我实现的是 `POST /api/payments`（body 传 `{orderId, channel}`）。两者对不上，**该按钮现在点了会 404**。

两个方案，选一个统一（我倾向方案 A，改动最小）：

- **方案 A**：前端改 1 行 → `request.post('/payments', { orderId: o.id, channel: 'WECHAT' })`
- **方案 B**：我在 `PaymentController` 加一个别名接口 `/pay/{orderId}`（5 行代码）

这个请组长拍板，或者 C/D 谁改 Orders.vue 顺手改掉。

---

## 七、推荐的合并顺序（照做基本不会冲突）

```
① 组长 review 并合并 B 的 PR #1（9 条提交）
        ↓
② C、D 各自执行（重要：必须在合并之后）
        git checkout main
        git pull origin main          ← 拉到含 B 代码的最新主干
        git checkout -b feat/m1-product   （C）
        git checkout -b feat/m0-auth      （D）
        ↓
③ 各自小步提交，push 自己的分支，提 PR（标题统一用 "feat: xxx"）
        ↓
④ 组长逐个 review 合并。谁后合并，谁负责解决共用文件冲突
```

### 冲突处理原则

1. **共用文件（`Constants` / `ResultCode` / `OrderMapper` / `application-dev.yml` / admin 三件套）改动前，先在群里说一句**
2. 遇到冲突，看清楚是"追加"还是"覆盖"——**一律保留双方内容，不要删别人的**
3. 不要互相 merge 别人的分支，一律走 PR 让组长把关

---

## 八、一句话总结（群里直接发这段就行）

> C、D 同学，我（B）的 M2/M4/M8 已完成并提交 PR #1（9 条提交 / 31 文件），等组长合并后你们再从最新 main 拉分支，能避开大部分冲突。
> 我占用的文件清单和 4 个合并热点（Constants / ResultCode / OrderMapper / application-dev.yml）都写在文档里了，改之前扫一眼。
> 另外两件事要同步：
> ① 首页等我（C）的商品和自提点接口，`/api/activities` 返回的是 `{total, list}` 分页结构，不是数组；
> ② 居民端 Orders.vue 的在线支付按钮调的是 `/payments/pay/{id}`，我实现的是 `POST /api/payments`，需要对齐一下。
> 完整说明在：`给C和D的接口与协作说明.md`
