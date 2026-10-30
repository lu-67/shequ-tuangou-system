import { createRouter, createWebHistory } from 'vue-router'
import WebLayout from '../layout/WebLayout.vue'
import Home from '../views/Home.vue'
import Orders from '../views/Orders.vue'
import Mine from '../views/Mine.vue'
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
      { path: 'mine', name: 'Mine', component: Mine, meta: { title: '个人中心' } }
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
  const token = localStorage.getItem('token')

  if (to.path !== '/login' && !token) {
    // 没登录，且想访问首页/订单/个人中心 -> 强制踢到登录页
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已经登录了，还想去登录页 -> 自动跳回首页
    next('/')
  } else {
    // 有 Token 正常放行
    next()
  }
})

export default router