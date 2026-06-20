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
      { path: 'merchant/products/create', name: 'MerchantProductCreate', component: () => import('@/views/product/form.vue'), meta: { title: '新增商品' } },
      { path: 'merchant/products/:id/edit', name: 'MerchantProductEdit', component: () => import('@/views/product/form.vue'), meta: { title: '编辑商品' } },
      { path: 'merchant/brands', name: 'MerchantBrands', component: () => import('@/views/brand/index.vue'), meta: { title: '品牌管理' } },
      { path: 'merchant/reviews', name: 'MerchantReviews', component: () => import('@/views/review/index.vue'), meta: { title: '评论管理' } },
      { path: 'merchant/knowledge', name: 'MerchantKnowledge', component: () => import('@/views/knowledge/documents.vue'), meta: { title: '知识库' } },
      { path: 'merchant/knowledge/chat', name: 'MerchantKnowledgeChat', component: () => import('@/views/knowledge/chat.vue'), meta: { title: '知识问答工作台' } },
      { path: 'merchant/inventory', name: 'MerchantInventory', component: () => import('@/views/inventory/index.vue'), meta: { title: '库存管理' } },
      { path: 'merchant/coupons', name: 'MerchantCoupons', component: () => import('@/views/coupon/list.vue'), meta: { title: '优惠券管理' } },
      { path: 'merchant/seckill', name: 'MerchantSeckill', component: () => import('@/views/seckill/list.vue'), meta: { title: '秒杀管理' } },
      { path: 'merchant/orders', name: 'MerchantOrders', component: () => import('@/views/merchant/orders.vue'), meta: { title: '订单管理' } },
      { path: 'merchant/payments', name: 'MerchantPayments', component: () => import('@/views/payment/list.vue'), meta: { title: '支付管理' } },
      { path: 'merchant/reconciliation', name: 'MerchantReconciliation', component: () => import('@/views/reconciliation/index.vue'), meta: { title: '对账管理' } },
      { path: 'merchant/reconciliation/:id', name: 'MerchantReconciliationDetail', component: () => import('@/views/reconciliation/detail.vue'), meta: { title: '对账明细' } },
      { path: 'merchant/settlement', name: 'MerchantSettlement', component: () => import('@/views/settlement/index.vue'), meta: { title: '结算管理' } },
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
      { path: 'knowledge/chat', name: 'KnowledgeChat', component: () => import('@/views/knowledge/chat.vue'), meta: { title: '知识问答工作台' } },
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
      { path: 'member/levels', name: 'MemberLevels', component: () => import('@/views/member/levels.vue'), meta: { title: '会员等级' } },
      { path: 'member/profiles', name: 'MemberProfiles', component: () => import('@/views/member/profiles.vue'), meta: { title: '会员列表' } },
      { path: 'member/points', name: 'MemberPoints', component: () => import('@/views/member/points.vue'), meta: { title: '积分流水' } },
      { path: 'logistics/providers', name: 'LogisticsProviders', component: () => import('@/views/logistics/providers.vue'), meta: { title: '物流公司', icon: 'Van' } },
      { path: 'logistics/shipping', name: 'LogisticsShipping', component: () => import('@/views/logistics/shipping.vue'), meta: { title: '发货单管理', icon: 'List' } },
      { path: 'warehouse/list', name: 'WarehouseList', component: () => import('@/views/warehouse/list.vue'), meta: { title: '仓库管理', icon: 'OfficeBuilding' } },
      { path: 'warehouse/inbound', name: 'WarehouseInbound', component: () => import('@/views/warehouse/inbound.vue'), meta: { title: '入库单', icon: 'Download' } },
      { path: 'warehouse/outbound', name: 'WarehouseOutbound', component: () => import('@/views/warehouse/outbound.vue'), meta: { title: '出库单', icon: 'Upload' } },
      { path: 'warehouse/stock', name: 'WarehouseStock', component: () => import('@/views/warehouse/stock.vue'), meta: { title: '库存查询', icon: 'Box' } },
      { path: 'warehouse/check', name: 'WarehouseCheck', component: () => import('@/views/warehouse/check.vue'), meta: { title: '盘点管理', icon: 'Check' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const type = localStorage.getItem('type')

  if (!to.meta.noAuth && !token) {
    next('/login')
  } else {
    if (type === 'merchant' && !to.meta.noAuth) {
      if (to.path === '/dashboard') {
        next('/merchant/dashboard')
        return
      }
      if (!to.path.startsWith('/merchant/')) {
        next('/merchant/dashboard')
        return
      }
    }

    if (to.path === '/dashboard') {
      if (type === 'merchant') next('/merchant/dashboard')
      else next()
    } else {
      next()
    }
  }
})

export default router
