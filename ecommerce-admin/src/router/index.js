import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据概览' } },
      { path: 'products', name: 'Products', component: () => import('@/views/product/list.vue'), meta: { title: '商品管理' } },
      { path: 'products/create', name: 'ProductCreate', component: () => import('@/views/product/form.vue'), meta: { title: '新增商品' } },
      { path: 'products/:id', name: 'ProductDetail', component: () => import('@/views/product/detail.vue'), meta: { title: '商品详情' } },
      { path: 'products/:id/edit', name: 'ProductEdit', component: () => import('@/views/product/form.vue'), meta: { title: '编辑商品' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/category/index.vue'), meta: { title: '分类管理' } },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/inventory/index.vue'), meta: { title: '库存管理' } },
      { path: 'users', name: 'Users', component: () => import('@/views/user/list.vue'), meta: { title: '用户管理' } },
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
    next()
  }
})

export default router
