<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">用户管理</span>
        <p class="page-desc">查看平台注册用户列表</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="200">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户名" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">
                {{ row.username?.charAt(0)?.toUpperCase() }}
              </div>
              <div class="user-info">
                <span class="user-name">{{ row.username }}</span>
                <span v-if="row.phone" class="user-phone">{{ row.phone }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="160">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:13px;color:var(--text-secondary)">{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/users', {
      params: { page: page.value, size: size.value }
    })
    if (data.code === 200) {
      users.value = data.data?.records || []
      total.value = data.data?.total || 0
    }
  } finally { loading.value = false }
}

fetchData()
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-desc {
  font-size: 13.5px;
  color: var(--text-muted);
  margin-top: 4px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  flex-direction: column;
}
.user-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}
.user-phone {
  font-size: 11.5px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.id-text {
  font-size: 12px;
  color: var(--text-muted);
}
.time-text {
  font-size: 12px;
  color: var(--text-secondary);
}
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
