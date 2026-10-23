# 社区团购 · 居民网页端（web）

居民视角的**网页端**（PC 布局），与 `admin/`（管理后台）、`community-groupbuy/`（后端）相互独立。

- 技术栈：Vue 3.4 + Vite 5 + Element Plus 2.7 + Pinia + Vue Router 4 + Axios
- 端口：**5174**（管理后台为 5173，后端为 8080）
- 跨域：前端使用相对路径 `/api`，由 Vite `server.proxy` 转发到 `http://localhost:8080`，无需后端配 CORS

## 页面

| 路由 | 页面 | 功能 | 状态 |
| --- | --- | --- | --- |
| `/home` | 团购首页 | 横幅、搜索、分类筛选、活动卡片网格、活动详情、参团下单 | ✅ 已实现 |
| `/orders` | 我的订单 | 按状态筛选、在线支付、查看/复制自提核销码 | ✅ 已实现 |
| `/mine` | 个人中心 | 账号信息、订单统计、进入管理后台（管理员/团长可见） | ✅ 已实现 |
| `/login` | 登录 | JWT 令牌存 localStorage | 待建（D：M0 登录注册与权限） |

## 启动

```bash
cd web
npm install
npm run dev
```

浏览器访问 `http://localhost:5174`。

> **当前状态（2026-10-23）**：登录页待 D 的 M0 登录注册与权限模块落地后开发，
> 当前页面可直接打开浏览；「我的订单」等需要登录态的接口在 M0 落地前返回 401。
> 居民演示账号：`user05` ~ `user15`，密码统一 `123456`。

## 目录结构

```
web/
├─ package.json
├─ vite.config.js           # 端口 5174；代理 /api → http://localhost:8080
├─ index.html
└─ src/
   ├─ main.js               # 入口（注册 Element Plus 与图标）
   ├─ App.vue
   ├─ constants.js          # 分类/状态映射（与后端 data.sql 一致）
   ├─ style.css             # 全局样式（登录页、网页端通用容器）
   ├─ layout/WebLayout.vue  # 顶部导航布局（品牌 + 菜单 + 用户下拉 + 页脚）
   ├─ router/index.js       # 路由与登录守卫
   ├─ stores/user.js        # Pinia：token 与用户信息
   ├─ utils/request.js      # Axios 封装（后端统一响应 {code,message,data}）
   └─ views/
      ├─ Home.vue           # 团购首页（卡片网格）
      ├─ Orders.vue         # 我的订单
      └─ Mine.vue           # 个人中心
      （Login.vue 由 D 的 M0 登录模块开发）
```

## 说明

- 依赖后端 `community-groupbuy/` 已启动（8080），使用 `src/main/resources/db/data.sql` 的演示数据。
- 居民演示账号：`user05` ~ `user15`，密码统一 `123456`。
- 管理员/团长账号（`admin` / `head1`~`head3`）登录本端后，"个人中心"会出现「进入管理后台」按钮，跳转到 `http://localhost:5173`（可在 `src/constants.js` 的 `ADMIN_URL` 修改）。
