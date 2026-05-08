<template>
  <div>
    <div class="toolbar">
      <span class="section-label">注册用户</span>
    </div>

    <el-table :data="users" class="data-table" v-loading="loading">
      <el-table-column prop="id" label="ID" width="180">
        <template #default="{ row }">
          <span class="font-mono" style="font-size:12px;color:var(--text-muted)">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px">
            <span class="user-avatar">{{ row.username?.charAt(0)?.toUpperCase() }}</span>
            {{ row.username }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="createdAt" label="注册时间" width="180">
        <template #default="{ row }">
          <span class="font-mono" style="font-size:12px;color:var(--text-secondary)">{{ row.createdAt }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const users = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/users')
    if (data.code === 200) users.value = data.data
  } finally { loading.value = false }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.font-mono { font-family: var(--font-mono); }
.user-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: var(--bg-surface);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
</style>
