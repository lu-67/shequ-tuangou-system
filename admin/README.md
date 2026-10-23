# 社区团购拼团系统 · 管理后台（admin）

Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios 实现的管理后台前端，
调用同项目 `community-groupbuy/` 的后端接口（Spring Boot 3）。

> 居民网页端在独立工程 `../web/`（端口 5174），本工程只承载**管理员/团长**视角。

## 当前状态与页面分工（2026-10-23）

按小组分工，本工程当前已完成**组长 A（工程骨架）**产出的部分：
登录态 store、路由骨架、侧边栏布局、请求封装。各模块业务页面由对应组员开发：

| 页面（`src/views/`） | 路由 | 负责人 | 依赖后端接口 | 状态 |
| --- | --- | --- | --- | --- |
| `Login.vue` | `/login` | D（M0 登录注册与权限） | `POST /api/auth/login` | 待建 |
| `Dashboard.vue` | `/dashboard` | C · D（M1 商品 + M6 统计） | `GET /api/stats/summary` | 待建 |
| `Product.vue` | `/products` | C（M1 商品管理） | `/api/products` | 待建 |
| `Activity.vue` | `/activities` | B（M2 开团活动） | `/api/activities` | 待建 |
| `Order.vue` | `/orders` | C · D（M5/M6 订单导出） | `/api/stats/*` | 待建 |
| `Verification.vue` | `/verification` | C（M5 自提核销） | `POST /api/pickup/verify` | 待建 |

## 页面接入方式（三处联动）

```
1. src/views/         新建页面组件（如 Product.vue）
2. src/router/index.js 在骨架的 children 数组里注册路由（文件内有对应组员的注释标记）
3. src/layout/Layout.vue 在侧边栏菜单加 el-menu-item（同样有按负责人分的注释标记）
```

## 技术栈

| 端 | 技术 |
| --- | --- |
| 框架 | Vue 3.4（Composition API + `<script setup>`） |
| 构建 | Vite 5 |
| UI | Element Plus 2.7 + @element-plus/icons-vue |
| 状态管理 | Pinia（登录态 / token 持久化到 localStorage） |
| 路由 | Vue Router 4（路由守卫，未登录跳登录页） |
| 请求 | Axios（统一封装，自动携带 `Authorization: Bearer <token>`） |

## 目录结构

```
admin/
├─ package.json
├─ vite.config.js           # 开发服务器代理：/api → http://localhost:8080（端口 5173）
├─ index.html
└─ src/
   ├─ main.js               # 应用入口（注册 Element Plus 与图标）
   ├─ App.vue
   ├─ constants.js          # 状态/分类/自提点映射（与后端 data.sql 一致）
   ├─ layout/Layout.vue     # 管理后台布局（侧边栏菜单 + 顶栏用户信息/退出）
   ├─ router/index.js       # 路由表与登录守卫（children 当前为空，待各组员注册）
   ├─ stores/user.js        # Pinia：token 与用户信息
   ├─ utils/request.js      # Axios 封装（后端统一响应 {code,message,data}）
   └─ views/                # 业务页面，当前为空目录（待 B/C/D 新建）
```

## 环境要求

- Node.js 16+（推荐 18/20）
- npm 9+
- **后端已启动**（默认 http://localhost:8080，`community-groupbuy/` 目录 `mvn spring-boot:run`）

## 快速开始

```bash
cd admin
npm install        # 首次安装依赖
npm run dev        # 启动，默认 http://localhost:5173
```

当前骨架可直接打开查看布局（菜单为占位提示），登录等页面待 D 的 M0 模块落地后即可联调。
演示账号沿用 `data.sql`：管理员 `admin`、团长 `head1`/`head2`/`head3`，密码统一 `123456`。

> 商品/活动写操作、订单统计与导出接口由后端 RBAC 控制（`@PreAuthorize("hasRole('ADMIN')")`），
> 非管理员账号操作相关按钮会返回无权限，属于正常表现。
>
> 居民账号请改用居民网页端 `../web/`（http://localhost:5174）。

## 跨域说明

前端一律用**相对路径** `/api` 请求，由 `vite.config.js` 的 `server.proxy` 转发到
`http://localhost:8080`，因此开发期不涉及跨域，也不需要在后端配置 CORS。

若把前端打包后单独用 Nginx 部署（前后端不同域），需要在后端 `SecurityConfig` 中补一个
`CorsConfigurationSource`（允许前端域名），或用 Nginx 做反向代理统一域名。

## 生产构建

```bash
npm run build      # 产物在 dist/
npm run preview    # 本地预览打包结果
```
