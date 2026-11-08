# 组员协作与 AI 提示词手册

> 组长 A 编写 · 2026-10-23 初版 / 2026-10-25 补充协作流程（分支保护 + PR review）· 配合仓库 `main` 分支（工程骨架 + M3 参团下单）使用

---

## 一、仓库地址与起步

远程仓库：**https://github.com/lu-67/shequ-tuangou-system**（公开，直接 clone）

```bash
git clone https://github.com/lu-67/shequ-tuangou-system.git
cd shequ-tuangou-system
```

> ⚠️ 旧地址 `github.com/lu-67/community-groupbuy` **已废弃**，不要再用。

目录结构：

| 目录 | 内容 | 负责人 |
| --- | --- | --- |
| `community-groupbuy/` | 后端 Spring Boot 3.2（端口 8080） | A 已完成骨架；B/C/D 补各自模块 |
| `admin/` | 管理后台前端 Vue3（端口 5173） | B/C/D |
| `web/` | 居民网页端 Vue3（端口 5174） | 接口等 B/C/D |

### 协作方式：分支 + Pull Request（不是各自直推 main）

`main` 分支已开启**分支保护**：任何人（含管理员）都不能直接 push 到 `main`，禁止强推、禁止删分支。
所有代码必须走 **个人分支 → 提 PR → 组长 review → 合并**。这就是老师要看的"Git 规范协作"证据。

**第 1 步：接受协作者邀请（一次性）**

组长已在 `Settings → Collaborators` 邀请你的 GitHub 账号。打开
`https://github.com/lu-67/shequ-tuangou-system/invitations` 点 **Accept invitation**。
接受后你就有 push 权限，可以建分支、推自己的分支、提 PR。

**第 2 步：建自己的分支**

```bash
git checkout -b feat/m2-activity     # B：开团活动
git checkout -b feat/m5-verify        # C：自提核销
git checkout -b feat/m0-auth          # D：用户权限
```

**第 3 步：提交并推自己的分支**

```bash
git add .
git commit -m "feat: 开团活动创建接口"
git push -u origin feat/m2-activity
```

提交信息规范：`feat: 新功能` / `fix: 修复` / `test: 测试` / `docs: 文档` / `refactor: 重构`

**第 4 步：提 PR 给组长 review**

命令行方式：

```bash
gh pr create --base main --head feat/m2-activity \
  --title "feat: M2 开团活动模块" \
  --body "实现活动创建/状态流转/成团判定，含 6 个单测，接口见 /doc.html"
```

或直接打开仓库页面 → **Compare & pull request** → 选自己的分支 → 提 PR。

**第 5 步：组长 review 后合并**

组长 review 通过 → `Merge pull request` → 你的分支会自动从 GitHub 上删除（本地可 `git branch -d` 清理）。

**第 6 步：同步最新 main**

```bash
git checkout main
git pull origin main
git checkout feat/m2-activity
git merge main        # 或 git rebase origin/main
```

### 冲突处理

```bash
git pull --rebase origin main
# 手动解决冲突文件后
git add <已解决的文件>
git rebase --continue
```

### 禁忌

- ❌ 不要直接 `git push origin main`（会被保护规则拒绝）
- ❌ 不要 `git push --force`（被禁止）
- ❌ 不要把自己模块的代码推到 `main` 分支
- ❌ 一个模块一个分支，别把三个模块混在一个分支里（PR 无法 review）
- ❌ 不要提交 `target/`、`node_modules/`、`.idea/`、密码配置


## 二、项目约定（所有 AI 生成代码必须遵守）

1. 技术栈：JDK 17 + Spring Boot 3.2（**jakarta** 命名空间，不是 javax）+ MyBatis + MySQL 8.0（utf8mb4）+ Redis 7 + Spring Security + JWT + Knife4j
2. 分层：controller / service / mapper / entity / dto / config / common，禁止跨层调用
3. 统一响应：`Result<T>`（code/message/data），异常抛 `BusinessException(ResultCode.XXX)`，由全局异常处理器兜底
4. 参数校验用 `jakarta.validation`（`@Valid` + `@NotNull/@Min` 等）
5. 日期时间：实体 `LocalDateTime` 加 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`；前端 el-date-picker 用 `value-format="YYYY-MM-DD HH:mm:ss"`
6. 事务：跨表写操作必须 `@Transactional(rollbackFor = Exception.class)`
7. Redis 全部 **fail-open**：Redis 不可用时降级，不阻断主流程
8. 数据库：`src/main/resources/db/schema.sql` 已建好 9 张表，**不要改表结构**，需要字段先找组长
9. 单方法 ≤80 行、嵌套 ≤3 层、魔法值抽到 `common/Constants.java`
10. 每个模块提交时必须带测试：Service 单测（JUnit5 + Mockito）≥ 该模块接口数的 1/2，含 ≥2 个异常分支；涉及接口的加 MockMvc 集成测试
11. **禁止提交**：`target/`、`node_modules/`、`dist/`、本地配置文件（.gitignore 已屏蔽）

---

## 三、给每个组员的 AI 提示词（可直接复制使用）

> 用法：clone 仓库后，把下面**对应你分工**的提示词整段贴给你使用的 AI（ChatGPT/Claude/文心/通义 等均可），让它基于仓库现有代码生成。提示词已包含全部必要上下文。

### B 同学：开团活动（M2）+ 支付成团（M4）+ 定时任务（M8）

```
我在做一个课程项目「社区团购（生鲜自提）拼团系统」，技术栈：JDK17 + Spring Boot 3.2（jakarta 命名空间）
+ MyBatis + MySQL 8.0 + Redis 7 + Spring Security + Knife4j，Maven 构建。代码已 clone 到本地，
工程骨架和「参团下单 M3」已由组长完成，我负责以下三个模块，请基于现有代码风格生成：

【现有约定（必须遵守）】
- 分层：controller/service/mapper/entity/dto，已有代码在 community-groupbuy/src/main/java/com/community/groupbuy/ 下，
  请先读 OrderController/OrderService/OrderMapper 学习现有风格
- 统一响应 Result<T>，业务异常抛 BusinessException(ResultCode.XXX)（已存在于 common 包）
- 跨表写必须 @Transactional(rollbackFor = Exception.class)；Redis 操作 fail-open 降级
- 实体时间字段用 LocalDateTime + @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
- 单方法≤80行、嵌套≤3层；数据库表已建好不要改结构（db/schema.sql）
- 模块从零实现：仓库只有组长的骨架代码可参考，表结构以 db/schema.sql 为准（不要改），接口风格对齐 OrderController/OrderService

【M2 开团活动】
- 实体/Mapper：ActivityMapper 已有 selectById/deductStockCAS/incrementStock（归我用），
  需补充 insert / updateStatus / selectOngoing / 动态条件查询（XML 手写动态 SQL，满足项目≥3处要求之一）
- 接口：POST /api/activities（创建，团长/管理员）、PUT /api/activities/{id}/status（状态流转）、
  GET /api/activities（分页+动态条件）、GET /api/activities/ongoing（进行中列表，首页用）
- 活动状态机：PENDING → ONGOING → SUCCESS / FAILED，用枚举+转移表+断言实现，非法流转抛业务异常

【M4 支付与成团】
- 接口：POST /api/payments（支付下单，幂等：同一订单重复支付返回同一结果）、
  POST /api/payments/callback（模拟支付回调，验签+幂等：已支付订单再次回调直接返回成功不重复入账）
- 支付成功后：订单 UNPAID→PAID、活动已参团人数+1、达到 total_stock 或到期后由 M8 判定 SUCCESS/FAILED
- 退款：POST /api/payments/{id}/refund，回补活动库存（调用已有的 incrementStock），订单→CANCELLED，全程同一事务

【M8 定时任务】
- 用 Spring @Scheduled：到期活动自动 SUCCESS/FAILED；超时未支付订单自动取消并回补库存
- 任务要加日志（SLF4J），幂等可重复执行

【测试要求】
- 每个 Service 写 JUnit5+Mockito 单测（≥6 例，含≥2 异常分支：如重复支付、回调重放、库存不足）
- 支付回调写 MockMvc 集成测试：验签失败/重复回调/正常回调 三种场景
- mvn test 必须全绿才算完成
```

### C 同学：自提核销（M5）+ 缓存（M7）+ 商品管理（M1）

```
我在做一个课程项目「社区团购（生鲜自提）拼团系统」，技术栈：JDK17 + Spring Boot 3.2（jakarta 命名空间）
+ MyBatis + MySQL 8.0 + Redis 7 + Spring Security + Knife4j，Maven 构建。代码已 clone 到本地，
工程骨架和「参团下单 M3（含 Lua 令牌桶限流、Redis 库存预扣调用）」已由组长完成，我负责以下三个模块，
请基于现有代码风格生成：

【现有约定（必须遵守）】
- 分层：controller/service/mapper/entity/dto，已有代码在 community-groupbuy/src/main/java/com/community/groupbuy/ 下，
  请先读 OrderService/RateLimitService 学习现有风格（限流是 Lua 脚本 + fail-open）
- 统一响应 Result<T>，业务异常抛 BusinessException(ResultCode.XXX)
- 跨表写必须 @Transactional；Redis 全部 fail-open
- 实体时间字段 LocalDateTime + @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
- 单方法≤80行、嵌套≤3层；表结构已建好（db/schema.sql）不要改
- 模块从零实现：仓库只有组长的骨架代码可参考，表结构以 db/schema.sql 为准（不要改），接口风格对齐 OrderController/OrderService

【M5 自提点与核销（验收硬指标：同一核销码连续核销5次，仅第1次成功）】
- 自提点：CRUD 接口 /api/pickup/points（GET 列表居民端首页要用，POST/PUT/DELETE 管理员）
- 核销：POST /api/pickup/verify（输入核销码+自提点），要求：
  1) 核销码不存在 → 业务异常；2) 订单必须已支付（PAID）；3) 订单已被核销（PICKED）→ 拒绝；
  4) 用数据库状态机保证幂等：UPDATE group_order SET status='PICKED' WHERE id=? AND status='PAID'，
     影响行数=0 即重复核销，抛业务异常。并发下仅一次成功，写 MockMvc 测试连续核销5次验证

【M7 缓存（验收硬指标：热点 key 过期瞬间100并发回源，DB 查询≤1次）】
- 商品/活动详情用 Cache-Aside：先读 Redis → 未命中查 DB → 回写 Redis（带随机 TTL 防雪崩）
- 缓存回源用 Redis 分布式锁（SETNX + Lua 释放）保证仅一个线程回源，其余等待后读缓存
- 注意：OrderService 里已有 Redis 库存预扣逻辑（stockKey 存在才预扣），但目前没有代码预热库存，
  请补上「活动创建/状态变为 ONGOING 时把 stock 写入 Redis」的预热（约20行，放在 M2 活动状态流转后调用），
  没有预热预扣就是空壳。预热失败不影响主流程

【M1 商品管理】
- 实体/Mapper/接口：/api/products CRUD + 分页动态查询（XML 手写动态 SQL，满足项目≥3处要求之一）
- 商品图片用 URL 字段存链接即可，不做文件上传

【测试要求】
- 每个 Service 写 JUnit5+Mockito 单测（商品≥4、核销≥6：含核销码不存在/未支付/重复核销等异常分支）
- 核销接口 MockMvc 集成测试：连续核销5次，断言仅1次成功
- mvn test 必须全绿才算完成
```

### D 同学：用户权限（M0）+ 统计导出（M6）+ 演示数据 + 报告

```
我在做一个课程项目「社区团购（生鲜自提）拼团系统」，技术栈：JDK17 + Spring Boot 3.2（jakarta 命名空间）
+ MyBatis + MySQL 8.0 + Redis 7 + Spring Security 6 + JWT + Knife4j + EasyExcel，Maven 构建。代码已 clone 到本地，
工程骨架和「参团下单 M3」已由组长完成，我负责以下模块，请基于现有代码风格生成：

【现有约定（必须遵守）】
- 分层：controller/service/mapper/entity/dto/config，已有代码在 community-groupbuy/src/main/java/com/community/groupbuy/ 下，
  请先读 SecurityConfig/SecurityUtils/JwtUser（config 包）——组长已留好占位
- 统一响应 Result<T>，业务异常抛 BusinessException(ResultCode.XXX)；全局异常已处理好参数校验/JSON解析失败
- 密码必须 BCrypt（SecurityConfig 里已有 passwordEncoder bean 直接用）
- 实体时间字段 LocalDateTime + @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
- 单方法≤80行、嵌套≤3层；表结构已建好（db/schema.sql，user 表已存在）不要改
- 模块从零实现：仓库只有组长的骨架代码可参考，表结构以 db/schema.sql 为准（不要改），接口风格对齐 OrderController/OrderService

【M0 用户与权限（最优先，全组等你）】
- 接口：POST /api/auth/register（注册，BCrypt 加密）、POST /api/auth/login（登录成功返回 JWT）、
  GET /api/auth/me（当前登录人信息）
- JWT：密钥走配置 jwt.secret（application-prod.yml 已用环境变量 JWT_SECRET），过期时间 24h
- 实现 JwtAuthFilter（解析 Authorization: Bearer <token>，把 JwtUser 放进 SecurityContext）
  + DbUserDetailsService，然后在 SecurityConfig 加过滤器链并把 permitAll 改回 authenticated()，
  仅放行 /api/auth/login、/api/auth/register、/doc.html、/swagger-ui/**、/v3/api-docs/**
- RBAC：角色 RESIDENT（居民）/ LEADER（团长）/ ADMIN（管理员），用 @PreAuthorize 控制接口
  （如创建商品、核销、统计仅管理员/团长）
- 注意：组长的 OrderController 已用 SecurityUtils.getUserId() 从 SecurityContext 取登录人，
  你的过滤器把 JwtUser 设进去即可，不要改 OrderController

【M6 统计导出】
- 接口：GET /api/stats/overview（订单数/销售额/成团数等汇总）、GET /api/stats/orders/export
  （EasyExcel 导出，≥300 行演示数据，管理员/团长可访问）
- 统计 SQL 走手写动态 SQL（XML，满足项目≥3处要求之一）

【演示数据 data.sql】
- db/data.sql 已有每表≥15条基础数据；请补充到「统计导出≥300行订单」的规模（可用存储过程或脚本生成，
  注意 order_no 和 verification_code 有唯一索引不能重复）

【测试要求】
- M0：MockMvc 集成测试 ≥4 例，必须含：未登录访问受保护接口返回 401、错误 token 返回 401、
  居民访问管理员接口返回 403（越权）、正常登录拿 token 后访问成功
- M6：Service 单测 ≥2 例
- mvn test 必须全绿才算完成

【另外】项目报告书模板在项目根目录，最后负责汇总各组内容成稿（组长会先填自己部分）
```

---

环境启动（每人本地）：

```bash
# 后端（MySQL 3307 端口、Redis 6379 先启动；无 Redis 也能跑，自动降级）
cd community-groupbuy && mvn spring-boot:run
# 前端
cd admin && npm install && npm run dev      # 5173
cd web && npm install && npm run dev        # 5174
```
