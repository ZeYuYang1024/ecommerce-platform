<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">用户会员列表</span>
        <p class="page-desc">查看用户等级、成长值与积分余额</p>
      </div>
      <div class="toolbar-right">
        <el-select v-model="filterLevel" placeholder="筛选等级" clearable style="width: 140px" @change="fetchData">
          <el-option label="普通会员" value="REGULAR" />
          <el-option label="银卡会员" value="SILVER" />
          <el-option label="金卡会员" value="GOLD" />
          <el-option label="钻石会员" value="DIAMOND" />
        </el-select>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="profiles" v-loading="loading">
        <el-table-column label="用户ID" width="200">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前等级" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.level" :type="tagType(row.level.sortOrder)" effect="light">{{ row.level.name }}</el-tag>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="成长值" width="140">
          <template #default="{ row }">
            <div>
              <span class="font-mono value-text">{{ row.growthValue }}</span>
              <span v-if="row.nextLevelGrowth" class="progress-hint">&nbsp;/ {{ row.nextLevelGrowth }}</span>
            </div>
            <el-progress
              v-if="row.nextLevelGrowth"
              :percentage="Math.min(100, Math.round((row.growthValue / row.nextLevelGrowth) * 100))"
              :stroke-width="4"
              :show-text="false"
              style="margin-top: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column label="累计成长值" width="120">
          <template #default="{ row }">
            <span class="font-mono value-text">{{ row.totalGrowthValue }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可用积分" width="120">
          <template #default="{ row }">
            <span class="font-mono value-text accent-text">{{ row.availablePoints }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计获取" width="110">
          <template #default="{ row }">
            <span class="font-mono value-text">{{ row.totalEarnedPoints }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计消耗" width="110">
          <template #default="{ row }">
            <span class="font-mono value-text">{{ row.totalSpentPoints }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const profiles = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterLevel = ref('')

function tagType(order) {
  const map = { 1: '', 2: 'success', 3: 'warning', 4: 'danger' }
  return map[order] || ''
}

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterLevel.value) {
      params.levelCode = filterLevel.value
    }
    const { data } = await axios.get('/api/v1/admin/member/profiles', { params })
    if (data.code === 200) {
      profiles.value = data.data?.records || []
      total.value = Number(data.data?.total) || profiles.value.length
    }
  } finally {
    loading.value = false
  }
}

fetchData()
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.font-mono { font-family: var(--font-mono); font-size: 13px; }
.value-text { font-size: 14px; color: var(--text-primary); }
.id-text { font-size: 12px; color: var(--text-muted); }
.progress-hint { font-size: 12px; color: var(--text-muted); }
.accent-text { color: var(--accent); font-weight: 600; }
.empty-text { color: var(--text-muted); }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
