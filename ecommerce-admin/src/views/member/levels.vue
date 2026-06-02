<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">会员等级管理</span>
        <p class="page-desc">配置四个会员等级的成长值门槛、积分倍率与权益</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="levels" v-loading="loading">
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.sortOrder)" effect="light" size="large">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="levelCode" label="编码" width="100" />
        <el-table-column prop="growthThreshold" label="成长值门槛" width="120" />
        <el-table-column prop="pointsMultiplier" label="积分倍率" width="100" />
        <el-table-column prop="birthdayGiftPoints" label="生日赠积分" width="120" />
        <el-table-column prop="discountRate" label="专属折扣" width="100">
          <template #default="{ row }">
            <span class="font-mono">{{ row.discountRate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="包邮" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.freeShipping" :size="18" color="var(--accent)"><Select /></el-icon>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="优先客服" width="100">
          <template #default="{ row }">
            <el-icon v-if="row.prioritySupport" :size="18" color="var(--accent)"><Select /></el-icon>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="新品优先" width="100">
          <template #default="{ row }">
            <el-icon v-if="row.earlyAccess" :size="18" color="var(--accent)"><Select /></el-icon>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑等级" width="520px" destroy-on-close>
      <el-form v-if="form.id" :model="form" label-width="110px">
        <el-form-item label="等级名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="成长值门槛">
          <el-input-number v-model="form.growthThreshold" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="积分倍率">
          <el-input-number v-model="form.pointsMultiplier" :min="0" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="生日赠积分">
          <el-input-number v-model="form.birthdayGiftPoints" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="专属折扣">
          <el-input-number v-model="form.discountRate" :min="0" :max="1" :precision="2" :step="0.01" />
        </el-form-item>
        <el-form-item label="包邮">
          <el-switch v-model="form.freeShipping" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="优先客服">
          <el-switch v-model="form.prioritySupport" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="新品优先">
          <el-switch v-model="form.earlyAccess" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="等级图标 URL">
          <el-input v-model="form.iconUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLevel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const levels = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const form = ref({})

function tagType(order) {
  const map = { 1: '', 2: 'success', 3: 'warning', 4: 'danger' }
  return map[order] || ''
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/member/levels')
    if (data.code === 200) {
      levels.value = data.data || []
    }
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function saveLevel() {
  saving.value = true
  try {
    const { data } = await axios.put(`/api/v1/admin/member/levels/${form.value.id}`, {
      name: form.value.name,
      growthThreshold: form.value.growthThreshold,
      pointsMultiplier: form.value.pointsMultiplier,
      birthdayGiftPoints: form.value.birthdayGiftPoints,
      discountRate: form.value.discountRate,
      freeShipping: form.value.freeShipping,
      prioritySupport: form.value.prioritySupport,
      earlyAccess: form.value.earlyAccess,
      iconUrl: form.value.iconUrl,
      description: form.value.description
    })
    if (data.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

fetchData()
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.font-mono { font-family: var(--font-mono); font-size: 13px; color: var(--text-secondary); }
.empty-text { color: var(--text-muted); }
</style>
