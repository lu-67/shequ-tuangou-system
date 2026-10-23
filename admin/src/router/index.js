import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

/**
 * 管理后台路由（当前为骨架状态）。
 *
 * 方案①执行后，各模块页面由对应组员实现后在此注册：
 * - D 同学：登录页 /login（M0 用户与权限）+ 登录守卫
 * - C 同学：/products（M1 商品管理）、/verification（M5 自提核销）、/dashboard 与 /orders（M6 统计导出）
 * - B 同学：/activities（M2 开团活动）
 *
 * 页面写好后在 Layout.vue 的侧边栏菜单中同步加上对应入口。
 */
const routes = [
  {
    path: '/',
    component: Layout,
    children: []
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
