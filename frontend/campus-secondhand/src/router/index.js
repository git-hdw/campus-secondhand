import { createRouter, createWebHistory } from 'vue-router'
import { getProfile } from '../api/user'

const routes = [
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/forgot',
    name: 'ResetPassword',
    component: () => import('../views/ResetPassword.vue'),
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/Products.vue')
  },
  {
    path: '/browse',
    name: 'Browse',
    component: () => import('../views/Products.vue')
  },
  {
    path: '/post',
    name: 'PostProduct',
    component: () => import('../views/PostProduct.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/drafts',
    name: 'Drafts',
    component: () => import('../views/Drafts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/Messages.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    redirect: { path: '/profile', query: { tab: 'orders' } }
  },
  {
    path: '/checkout/:productId',
    name: 'OrderCheckout',
    component: () => import('../views/OrderCheckout.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders/:orderId/result',
    name: 'OrderResult',
    component: () => import('../views/OrderCheckout.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/help',
    name: 'Help',
    component: () => import('../views/Help.vue')
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue')
  },
  // 管理员路由
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/Admin/AdminUsers.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    component: () => import('../views/Admin/AdminProducts.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('../views/Admin/AdminOrders.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/categories',
    name: 'AdminCategories',
    component: () => import('../views/Admin/AdminCategories.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/messages',
    name: 'AdminMessages',
    component: () => import('../views/Admin/AdminMessages.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to) {
    if (to.hash) return { el: to.hash }
    return { top: 0 }
  }
})

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user') || '{}')
  } catch (e) {
    return {}
  }
}

function writeUser(user) {
  const box = localStorage.getItem('token') ? localStorage : sessionStorage
  box.setItem('user', JSON.stringify(user))
}

router.beforeEach(async (to, from, next) => {
  const requiresAuth = to.meta && to.meta.requiresAuth
  const requiresAdmin = to.meta && to.meta.requiresAdmin
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')

  if ((requiresAuth || requiresAdmin) && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (requiresAdmin) {
    try {
      const res = await getProfile()
      const role = res.data && res.data.role
      const saved = readUser()
      if (saved.role !== role) {
        saved.role = role
        writeUser(saved)
      }
      if (role !== 1) {
        next({ path: '/', query: { error: '权限不足' } })
        return
      }
    } catch (e) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

const pageTitles = { Home:'首页', Login:'登录', Register:'注册', Products:'商品', Browse:'商品', ProductDetail:'商品详情', OrderCheckout:'确认订单', OrderResult:'订单详情', PostProduct:'发布闲置', Messages:'消息', Profile:'个人中心', Drafts:'草稿箱', Help:'使用帮助', Admin:'管理概览', AdminUsers:'用户管理', AdminProducts:'商品管理', AdminOrders:'订单管理', AdminCategories:'分类管理', AdminMessages:'消息管理', ResetPassword:'密码说明', NotFound:'页面未找到' }
router.afterEach(to => { document.title = `${pageTitles[to.name] || '校园二手'} · 校园二手交易系统` })
export default router
