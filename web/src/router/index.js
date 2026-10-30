import { createRouter, createWebHistory } from 'vue-router'
import WebLayout from '../layout/WebLayout.vue'
import Home from '../views/Home.vue'
import Orders from '../views/Orders.vue'
import Mine from '../views/Mine.vue'
import ActivityCreate from '../views/ActivityCreate.vue'
import ActivityMine from '../views/ActivityMine.vue'
// 引入你等下要创建的登录页
import Login from '../views/Login.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '居民端登录' }
  },
  {
    // 居民网页端（顶部导航布局）
    path: '/',
    component: WebLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: Home, meta: { title: '团购首页' } },
      { path: 'orders', name: 'Orders', component: Orders, meta: { title: '我的订单' } },
      { path: 'mine', name: 'Mine', component: Mine, meta: { title: '个人中心' } },
      { path: 'my-activities', name: 'ActivityMine', component: ActivityMine, meta: { title: '我的开团', roles: ['ADMIN', 'HEAD'] } },
      { path: 'create-activity', name: 'ActivityCreate', component: ActivityCreate, meta: { title: '发起开团', roles: ['ADMIN', 'HEAD'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ================= 全局路由守卫（解决 M0 权限拦截） =================
router.beforeEach((to, from, next) => {
  // 与 user.js store 的 TOKEN_KEY='cg_token' 对齐
  const token = localStorage.getItem('cg_token')
  let userInfo = null
  try { userInfo = JSON.parse(localStorage.getItem('cg_user') || 'null') } catch (e) {}

  if (to.path !== '/login' && !token) {
    // 没登录，且想访问首页/订单/个人中心 -> 强制踢到登录页
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已经登录了，还想去登录页 -> 自动跳回首页
    next('/')
  } else if (to.meta.roles && userInfo && !to.meta.roles.includes(userInfo.role)) {
    // 需要特定角色，当前用户不满足 → 踢回首页
    next('/')
  } else {
    // 有 Token 正常放行
    next()
  }
})

export default router