<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
            <defs>
              <linearGradient id="logo-grad" x1="0" y1="0" x2="28" y2="28">
                <stop stop-color="#C8963E"/>
                <stop offset="1" stop-color="#E8C876"/>
              </linearGradient>
            </defs>
            <rect width="28" height="28" rx="8" fill="url(#logo-grad)"/>
            <path d="M8 10l6-4 6 4v8l-6 4-6-4V10z" stroke="#1A1816" stroke-width="1.8" fill="none"/>
            <circle cx="14" cy="14" r="2.5" fill="#1A1816"/>
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
          <img class="avatar-img" :src="avatarUrl" :alt="username" />
          <span class="user-name">{{ username }}</span>
        </div>
        <el-button text class="logout-btn" @click="logout">
          <el-icon :size="18"><SwitchButton /></el-icon>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '管理员')
const time = ref('')

const avatarUrl = computed(() => {
  const name = username.value || 'A'
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=C8963E&color=1A1816&size=64&font-size=0.4&bold=true&rounded=true`
})

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
  width: 260px;
  flex-shrink: 0;
  background: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  user-select: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 28px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.brand-icon svg { display: block; filter: drop-shadow(0 2px 8px rgba(200, 150, 62, 0.5)); }
.brand-text {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--text-on-dark);
}
.brand-accent { color: var(--accent); }

.nav {
  flex: 1;
  padding: 12px 12px 0;
  overflow-y: auto;
}
.nav-section {
  font-size: 10.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(148, 163, 184, 0.6);
  padding: 20px 12px 8px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius);
  color: var(--text-on-dark-muted);
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 500;
  margin-bottom: 2px;
  transition: all var(--transition-fast);
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-on-dark);
}
.nav-item.active {
  background: var(--accent-glow);
  color: var(--accent);
  font-weight: 600;
}

.sidebar-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.1);
}
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-on-dark);
}
.logout-btn {
  color: var(--text-on-dark-muted) !important;
  padding: 8px !important;
  border-radius: var(--radius) !important;
  transition: all var(--transition-fast);
}
.logout-btn:hover {
  color: var(--red) !important;
  background: rgba(239, 68, 68, 0.1) !important;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-card);
  box-shadow: var(--shadow-xs);
}
.crumb {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}
.clock {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}
</style>
