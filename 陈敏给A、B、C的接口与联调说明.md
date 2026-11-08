D 同学（M0 权限 + M6 统计导出）开发与联调说明

作者：成员 D（M0 用户权限 / M6 统计导出 / 演示数据与报告）

时间：2026-10-29

状态：代码已全部写完并自测通过，PR 已提交，等待组长合并。



一、当前进度与合并状态

模块	状态	说明

M0 用户与鉴权	✅ 已完成	注册、登录、JWT生成解析、过滤器鉴权、全局异常已通

M6 统计与导出	✅ 已完成	看板统计接口、EasyExcel 导出订单接口已通

演示数据 data.sql	✅ 已补充	已提供 320 条订单及对应核销记录，满足导出≥300行的要求

二、M0 权限模块：给 A、B、C 的联调须知

我已在 SecurityConfig 中把拦截器配置完毕，现在是无状态 JWT 鉴权，以下几点务必遵守：



角色标识必须用 HEAD：



我在代码里统一使用的是 ADMIN（管理员）、HEAD（团长）、RESIDENT（居民）。B 和 C 如果写了 LEADER 会报 403 越权错误，请务必改成 HEAD。



白名单已放行：



除了登录注册和 Knife4j 接口文档，我还为首页放行了 B 和 C 的 GET 接口（不需要 Token 即可访问）：



/api/activities/ongoing（B 的开团首页数据）



/api/products/list（C 的商品列表）



/api/pickup/points（C 的自提点列表）



接口测试方法：



现在所有业务接口都要求认证。大家在 Knife4j 测试时，请先调用 POST /api/auth/login（账号 admin，密码 123456）拿到 Token。



然后在“请求头部”中添加参数：Authorization，值为 Bearer <你的Token>（注意 Bearer 后有空格）。



三、M6 统计导出：接口契约与测试说明

看板统计 GET /api/stats/overview



返回格式：Result<Map>，包含 totalOrders（总订单）、totalSales（总销售额）。权限仅限 ADMIN 和 HEAD。



订单导出 GET /api/stats/orders/export



返回格式：这是直接返回文件流的接口。不要在 Knife4j 的响应面板里看（会是一堆 HTML 乱码），请直接在浏览器里输入带 Token 的 URL 测试。



四、⚠️ 环境配置与端口修改说明

数据库端口：本地测试时，因为环境冲突，我的 application-dev.yml 中数据库连接端口已改为了 3306（组长原版是 3307）。请组长合并代码时注意一下，或者大家拉取最新代码后，确认自己的本地 MySQL 端口与配置一致，以免报 Connection refused。



数据库自动重置：因为 application.yml 里开启了 spring.sql.init.mode=always，每次重启项目都会自动重置这 320 条演示数据。如果在录演示视频，不用担心数据越测越少。



五、后续工作提醒（给组长和全组）

合并冲突预警：我的 PR 修改了 pom.xml、data.sql、SecurityConfig 等，组长合并时请保留我的版本。



前端数据持久化：请做前端的同学注意，登录成功后需将 token 存入 localStorage，并配置 axios 拦截器，每次请求带上 Bearer 头。

