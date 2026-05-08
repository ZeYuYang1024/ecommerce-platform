<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="6" fill="#e6a820"/>
            <path d="M8 10l6-4 6 4v8l-6 4-6-4V10z" stroke="#000" stroke-width="1.5" fill="none"/>
            <circle cx="14" cy="14" r="2.5" fill="#000"/>
          </svg>
        </div>
        <span class="brand-text">MERCH<span class="brand-accent">PANEL</span></span>
      </div>

      <nav class="nav">
        <router-link to="/dashboard" class="nav-item" :class="{ active: $route.path === '/dashboard' }">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </router-link>

        <div class="nav-section">商品</div>

        <router-link to="/products" class="nav-item" :class="{ active: $route.path.startsWith('/products') }">
          <el-icon><Goods /></el-icon>
          <span>商品列表</span>
        </router-link>
        <router-link to="/categories" class="nav-item" :class="{ active: $route.path === '/categories' }">
          <el-icon><Grid /></el-icon>
          <span>分类管理</span>
        </router-link>

        <div class="nav-section">运营</div>

        <router-link to="/inventory" class="nav-item" :class="{ active: $route.path === '/inventory' }">
          <el-icon><Box /></el-icon>
          <span>库存管理</span>
        </router-link>
        <router-link to="/users" class="nav-item" :class="{ active: $route.path === '/users' }">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-chip">
          <span class="avatar">{{ username.charAt(0).toUpperCase() }}</span>
          <span class="user-name">{{ username }}</span>
        </div>
        <el-button text class="logout-btn" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <span class="crumb">{{ $route.meta.title }}</span>
        </div>
        <div class="topbar-right">
          <span class="clock">{{ time }}</span>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '管理员')
const time = ref('')

let timer
onMounted(() => {
  timer = setInterval(() => {
    time.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }, 1000)
})
onUnmounted(() => clearInterval(timer))

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  user-select: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 24px;
}
.brand-icon svg { display: block; }
.brand-text {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text-primary);
}
.brand-accent { color: var(--accent); }

.nav {
  flex: 1;
  padding: 0 12px;
  overflow-y: auto;
}
.nav-section {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-muted);
  padding: 16px 8px 6px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: 0.01em;
  margin-bottom: 2px;
  transition: all var(--transition);
  position: relative;
}
.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}
.nav-item.active {
  background: var(--accent-glow);
  color: var(--accent);
}

.sidebar-footer {
  border-top: 1px solid var(--border-subtle);
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #000;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}
.logout-btn {
  color: var(--text-muted) !important;
  font-size: 18px;
}
.logout-btn:hover { color: var(--red) !important; }

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-root);
}
.crumb {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.01em;
}
.clock {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-muted);
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
