import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/merchant/register',
    name: 'MerchantRegister',
    component: () => import('@/views/merchant/register.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据概览' } },
      { path: 'merchant/dashboard', name: 'MerchantDashboard', component: () => import('@/views/merchant/dashboard.vue'), meta: { title: '商家中心' } },
      { path: 'merchant/products', name: 'MerchantProducts', component: () => import('@/views/merchant/products.vue'), meta: { title: '商品管理' } },
      { path: 'merchant/orders', name: 'MerchantOrders', component: () => import('@/views/merchant/orders.vue'), meta: { title: '订单管理' } },
      { path: 'merchant/shop', name: 'MerchantShop', component: () => import('@/views/merchant/shop.vue'), meta: { title: '店铺信息' } },
      { path: 'merchants', name: 'Merchants', component: () => import('@/views/merchant/list.vue'), meta: { title: '商家管理' } },
      { path: 'merchants/:id', name: 'MerchantDetail', component: () => import('@/views/merchant/detail.vue'), meta: { title: '商家详情' } },
      { path: 'products', name: 'Products', component: () => import('@/views/product/list.vue'), meta: { title: '商品管理' } },
      { path: 'products/create', name: 'ProductCreate', component: () => import('@/views/product/form.vue'), meta: { title: '新增商品' } },
      { path: 'products/:id', name: 'ProductDetail', component: () => import('@/views/product/detail.vue'), meta: { title: '商品详情' } },
      { path: 'products/:id/edit', name: 'ProductEdit', component: () => import('@/views/product/form.vue'), meta: { title: '编辑商品' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/category/index.vue'), meta: { title: '类目管理' } },
      { path: 'brands', name: 'Brands', component: () => import('@/views/brand/index.vue'), meta: { title: '品牌管理' } },
      { path: 'reviews', name: 'Reviews', component: () => import('@/views/review/index.vue'), meta: { title: '评论管理' } },
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/knowledge/documents.vue'), meta: { title: '知识库' } },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/inventory/index.vue'), meta: { title: '库存管理' } },
      { path: 'coupons', name: 'Coupons', component: () => import('@/views/coupon/list.vue'), meta: { title: '优惠券管理' } },
      { path: 'seckill', name: 'Seckill', component: () => import('@/views/seckill/list.vue'), meta: { title: '秒杀管理' } },
      { path: 'orders', name: 'Orders', component: () => import('@/views/order/list.vue'), meta: { title: '订单管理' } },
      { path: 'payments', name: 'Payments', component: () => import('@/views/payment/list.vue'), meta: { title: '支付管理' } },
      { path: 'reconciliation', name: 'Reconciliation', component: () => import('@/views/reconciliation/index.vue'), meta: { title: '对账管理' } },
      { path: 'reconciliation/:id', name: 'ReconciliationDetail', component: () => import('@/views/reconciliation/detail.vue'), meta: { title: '对账明细' } },
      { path: 'settlement', name: 'Settlement', component: () => import('@/views/settlement/index.vue'), meta: { title: '日终结算' } },
      { path: 'users', name: 'Users', component: () => import('@/views/user/list.vue'), meta: { title: '用户管理' } },
      { path: 'roles', name: 'Roles', component: () => import('@/views/role/index.vue'), meta: { title: '角色管理' } },
      { path: 'permissions', name: 'Permissions', component: () => import('@/views/permission/index.vue'), meta: { title: '权限管理' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.noAuth && !token) {
    next('/login')
  } else {
    if (to.path === '/dashboard') {
      const type = localStorage.getItem('type')
      if (type === 'merchant') next('/merchant/dashboard')
      else next()
    } else {
      next()
    }
  }
})

export default router
