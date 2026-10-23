import { createRouter, createWebHistory } from 'vue-router'
import WebLayout from '../layout/WebLayout.vue'
import Home from '../views/Home.vue'
import Orders from '../views/Orders.vue'
import Mine from '../views/Mine.vue'

/**
 * 居民网页端路由。
 *
 * 说明：登录页 /login 属于 M0 用户与权限（D 同学负责），
 * 待 D 同学实现后端 /api/auth/login 与 JWT 后，在此恢复登录路由与登录守卫：
 *   { path: '/login', component: Login }
 *   router.beforeEach：未登录访问非 /login 页面时跳转 /login
 */
const routes = [
  {
    // 居民网页端（顶部导航布局）
    path: '/',
    component: WebLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: Home, meta: { title: '团购首页' } },
      { path: 'orders', name: 'Orders', component: Orders, meta: { title: '我的订单' } },
      { path: 'mine', name: 'Mine', component: Mine, meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
