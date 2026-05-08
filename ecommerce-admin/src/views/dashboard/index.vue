<template>
  <div class="dashboard">
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(230,168,32,0.12)"><el-icon :size="22" color="#e6a820"><Goods /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value font-mono">--</div>
          <div class="stat-label">商品总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(45,212,191,0.12)"><el-icon :size="22" color="#2dd4bf"><ShoppingCart /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value font-mono">--</div>
          <div class="stat-label">今日订单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(96,165,250,0.12)"><el-icon :size="22" color="#60a5fa"><User /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value font-mono">--</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(248,113,113,0.12)"><el-icon :size="22" color="#f87171"><TrendCharts /></el-icon></div>
        <div class="stat-body">
          <div class="stat-value font-mono">--</div>
          <div class="stat-label">营收</div>
        </div>
      </div>
    </div>

    <div class="welcome-card">
      <div>
        <h2>{{ username }}，欢迎回来</h2>
        <p>电商管理后台 · P0 阶段。在此管理商品、库存和用户。</p>
      </div>
      <div class="quick-links">
        <el-button type="primary" @click="$router.push('/products/create')">新增商品</el-button>
        <el-button @click="$router.push('/products')">查看全部商品</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const username = ref(localStorage.getItem('username') || '管理员')
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: border-color var(--transition);
}
.stat-card:hover { border-color: var(--border-default); }
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  font-family: var(--font-mono);
}
.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  font-weight: 500;
}
.font-mono { font-family: var(--font-mono); }

.welcome-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.welcome-card h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.welcome-card p {
  font-size: 13px;
  color: var(--text-muted);
}
.quick-links { display: flex; gap: 10px; flex-shrink: 0; }
</style>
