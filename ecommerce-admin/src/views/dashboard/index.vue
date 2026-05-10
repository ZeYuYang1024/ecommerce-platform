<template>
  <div class="dashboard">
    <div class="greeting">
      <h1>{{ greeting }}，{{ username }}</h1>
      <p>平台管理后台 · 商家入驻审核</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card" @click="$router.push('/merchants?status=1')">
        <div class="stat-icon-box" style="background: rgba(200, 150, 62, 0.12); color: var(--accent);">
          <el-icon :size="24"><Shop /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value font-mono">{{ merchantCount }}</div>
          <div class="stat-label">入驻商家</div>
        </div>
        <div class="stat-spark"></div>
      </div>
      <div class="stat-card warn" @click="$router.push('/merchants?status=0')">
        <div class="stat-icon-box" style="background: rgba(217, 119, 6, 0.10); color: var(--orange);">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value font-mono" style="color: var(--orange);">{{ pendingAuditCount }}</div>
          <div class="stat-label">待审核</div>
        </div>
        <div class="stat-spark orange"></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background: rgba(5, 150, 105, 0.10); color: var(--green);">
          <el-icon :size="24"><Goods /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value font-mono">{{ productCount }}</div>
          <div class="stat-label">平台商品</div>
        </div>
        <div class="stat-spark green"></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-box" style="background: rgba(59, 130, 246, 0.10); color: var(--blue);">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value font-mono">{{ userCount }}</div>
          <div class="stat-label">注册用户</div>
        </div>
        <div class="stat-spark blue"></div>
      </div>
    </div>

    <!-- Bento-style second row -->
    <div class="bento-grid">
      <div class="quick-card">
        <div class="quick-info">
          <h2>快速操作</h2>
          <p>审核商家入驻 · 管理平台类目</p>
        </div>
        <div class="quick-links">
          <el-button type="primary" size="large" @click="$router.push('/merchants')">
            <el-icon style="margin-right:6px"><Shop /></el-icon> 商家审核
          </el-button>
          <el-button size="large" @click="$router.push('/categories')">
            <el-icon style="margin-right:6px"><Grid /></el-icon> 类目管理
          </el-button>
          <el-button size="large" @click="$router.push('/users')">
            <el-icon style="margin-right:6px"><User /></el-icon> 用户管理
          </el-button>
        </div>
      </div>

      <div class="info-card">
        <div class="info-icon">
          <el-icon :size="20"><InfoFilled /></el-icon>
        </div>
        <div class="info-body">
          <span class="info-title">系统状态</span>
          <span class="info-desc">所有服务运行正常 · P0 阶段</span>
        </div>
        <div class="info-status ok"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const username = ref(localStorage.getItem('username') || '管理员')
const merchantCount = ref('--')
const pendingAuditCount = ref('--')
const productCount = ref('--')
const userCount = ref('--')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  try {
    const { data } = await axios.get('/api/v1/admin/dashboard/stats')
    if (data.code === 200 && data.data) {
      merchantCount.value = data.data.merchantCount ?? 0
      pendingAuditCount.value = data.data.pendingAuditCount ?? 0
      productCount.value = data.data.productCount ?? 0
      userCount.value = data.data.userCount ?? 0
    }
  } catch {}
})
</script>

<style scoped>
.dashboard {
  max-width: 960px;
}

.greeting {
  margin-bottom: 32px;
}
.greeting h1 {
  font-size: 30px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.03em;
  margin-bottom: 6px;
}
.greeting p {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 500;
}

/* Stat cards — 4-col grid */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 18px;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-xs);
  position: relative;
  overflow: hidden;
  cursor: default;
}
.stat-card:has(.stat-icon-box) {
  cursor: pointer;
}
.stat-card:hover {
  border-color: var(--border-default);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.stat-icon-box {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-spark {
  position: absolute;
  right: -8px;
  bottom: -8px;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--accent-glow) 0%, transparent 70%);
  opacity: 0.4;
}
.stat-spark.green { background: radial-gradient(circle, rgba(5, 150, 105, 0.12) 0%, transparent 70%); }
.stat-spark.blue { background: radial-gradient(circle, rgba(59, 130, 246, 0.12) 0%, transparent 70%); }
.stat-spark.orange { background: radial-gradient(circle, rgba(217, 119, 6, 0.12) 0%, transparent 70%); }
.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.1;
}
.stat-label {
  font-size: 12.5px;
  color: var(--text-muted);
  margin-top: 3px;
  font-weight: 500;
}

/* Bento grid — 2 cards side by side */
.bento-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
}

.quick-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 32px 36px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-xs);
  transition: all var(--transition-fast);
}
.quick-card:hover {
  box-shadow: var(--shadow);
}
.quick-info h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.quick-info p {
  font-size: 13px;
  color: var(--text-muted);
}
.quick-links {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* System status card */
.info-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 24px 28px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-xs);
  transition: all var(--transition-fast);
}
.info-card:hover {
  box-shadow: var(--shadow);
}
.info-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  flex-shrink: 0;
}
.info-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.info-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.info-desc {
  font-size: 12px;
  color: var(--text-muted);
}
.info-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-left: auto;
}
.info-status.ok {
  background: var(--green);
  box-shadow: 0 0 8px rgba(5, 150, 105, 0.5);
  animation: pulse-status 2s ease-in-out infinite;
}
@keyframes pulse-status {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
}
</style>
