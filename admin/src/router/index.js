import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import Activity from '../views/Activity.vue'
import Product from '../views/Product.vue'
import Verification from '../views/Verification.vue'
// 引入你新建的 Login 页面
import Login from '../views/Login.vue'
// D 同学：M6 统计导出 —— 数据看板 + 订单管理（含导出）
import Dashboard from '../views/Dashboard.vue'
import Order from '../views/Order.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '系统登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: Dashboard, meta: { title: '数据看板' } },
      { path: 'activities', name: 'Activity', component: Activity, meta: { title: '开团活动管理' } },
      { path: 'products', name: 'Product', component: Product, meta: { title: '商品管理' } },
      { path: 'orders', name: 'Order', component: Order, meta: { title: '订单管理' } },
      { path: 'verification', name: 'Verification', component: Verification, meta: { title: '自提核销' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ================= 全局路由守卫 =================
router.beforeEach((to, from, next) => {
  // 获取本地存储的 token
  const token = localStorage.getItem('token')

  if (to.path !== '/login' && !token) {
    // 如果没登录，且去的不是登录页，强制跳到登录页
    next('/login')
  } else if (to.path === '/login' && token) {
    // 如果已经登录，还去登录页，直接放行到首页
    next('/')
  } else {
    // 正常放行
    next()
  }
})

export default router