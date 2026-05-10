<template>
  <div class="merchant-dashboard">
    <div class="greeting">
      <h1>商家中心</h1>
      <p>{{ shopName }} · 管理您的店铺</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon-box" style="background:rgba(5,150,105,0.1);color:#059669">
          <el-icon :size="24"><Goods /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ productCount }}</div>
          <div class="stat-label">在售商品</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background:rgba(59,130,246,0.1);color:#3b82f6">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ orderCount }}</div>
          <div class="stat-label">总订单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background:rgba(217,119,6,0.1);color:#d97706">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ pendingOrders }}</div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
    </div>

    <div class="quick-grid">
      <el-card shadow="never" class="quick-card" @click="$router.push('/merchant/products')">
        <el-icon :size="28"><Plus /></el-icon>
        <span>发布商品</span>
      </el-card>
      <el-card shadow="never" class="quick-card" @click="$router.push('/merchant/orders')">
        <el-icon :size="28"><Document /></el-icon>
        <span>查看订单</span>
      </el-card>
      <el-card shadow="never" class="quick-card" @click="$router.push('/merchant/shop')">
        <el-icon :size="28"><Setting /></el-icon>
        <span>店铺设置</span>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const shopName = ref('我的店铺')
const productCount = ref('--')
const orderCount = ref('--')
const pendingOrders = ref('--')

onMounted(async () => {
  try {
    const [pRes, oRes] = await Promise.all([
      axios.get('/api/v1/admin/products?page=1&size=1'),
      axios.get('/api/v1/admin/merchant/orders?page=1&size=1')
    ])
    if (pRes.data.code === 200) productCount.value = pRes.data.data.total || 0
    if (oRes.data.code === 200) orderCount.value = oRes.data.data.total || 0
  } catch {}
  // Count pending orders
  try {
    const { data } = await axios.get('/api/v1/admin/merchant/orders?page=1&size=1&status=0')
    if (data.code === 200) pendingOrders.value = data.data.total || 0
  } catch {}
})
</script>

<style scoped>
.merchant-dashboard { max-width: 800px; }
.greeting { margin-bottom: 32px; }
.greeting h1 { font-size: 28px; font-weight: 800; color: var(--text-primary); }
.greeting p { font-size: 14px; color: var(--text-muted); margin-top: 4px; }
.stat-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; margin-bottom: 28px; }
.stat-card { background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-xl); padding: 24px; display: flex; align-items: center; gap: 16px; }
.stat-icon-box { width: 48px; height: 48px; border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center; }
.stat-value { font-size: 28px; font-weight: 800; color: var(--text-primary); }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 2px; }
.quick-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; }
.quick-card { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 32px; cursor: pointer; transition: all var(--transition-fast); border-radius: var(--radius-xl); color: var(--text-secondary); }
.quick-card:hover { border-color: var(--accent); color: var(--accent); transform: translateY(-2px); box-shadow: var(--shadow); }
.quick-card span { font-weight: 600; font-size: 14px; }
</style>
