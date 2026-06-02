<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">积分流水</span>
        <p class="page-desc">查看全平台积分变动记录</p>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openGrant">手动发放积分</el-button>
      </div>
    </div>

    <div class="filter-row">
      <el-input v-model="filterUserId" placeholder="用户ID" clearable style="width:200px" @change="fetchData" />
      <el-select v-model="filterSource" placeholder="来源类型" clearable style="width:140px" @change="fetchData">
        <el-option label="订单" value="ORDER" />
        <el-option label="签到" value="CHECKIN" />
        <el-option label="评价" value="REVIEW" />
        <el-option label="营销活动" value="CAMPAIGN" />
        <el-option label="过期" value="EXPIRE" />
      </el-select>
    </div>

    <el-card shadow="never" style="margin-top:16px">
      <el-table :data="transactions" v-loading="loading">
        <el-table-column label="流水ID" width="200">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'EARN' ? 'success' : row.direction === 'SPEND' ? 'warning' : 'info'" size="small" effect="light">
              {{ dirLabel(row.direction) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }">
            <span class="font-mono" :style="{ color: row.direction === 'EARN' ? 'var(--green)' : 'var(--red)', fontWeight: 600 }">
              {{ row.direction === 'EARN' ? '+' : '-' }}{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后余额" width="120">
          <template #default="{ row }">
            <span class="font-mono value-text">{{ row.balanceAfter }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="100" />
        <el-table-column prop="sourceId" label="来源ID" width="140">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.sourceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ row.expireAt || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ row.createdAt }}</span>
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

    <el-dialog v-model="grantVisible" title="手动发放积分" width="440px" destroy-on-close>
      <el-form :model="grantForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="grantForm.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="积分数量">
          <el-input-number v-model="grantForm.amount" :min="1" :step="10" style="width:100%" />
        </el-form-item>
        <el-form-item label="来源标识">
          <el-input v-model="grantForm.sourceId" placeholder="如活动ID" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="grantForm.remark" placeholder="发放原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" @click="doGrant" :loading="granting">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const transactions = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterUserId = ref('')
const filterSource = ref('')

const grantVisible = ref(false)
const granting = ref(false)
const grantForm = ref({ userId: '', amount: 10, sourceId: '', remark: '' })

function dirLabel(d) {
  const map = { EARN: '获取', SPEND: '消耗', EXPIRE: '过期' }
  return map[d] || d
}

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterUserId.value) params.userId = filterUserId.value
    if (filterSource.value) params.sourceType = filterSource.value
    const { data } = await axios.get('/api/v1/admin/member/points/transactions', { params })
    if (data.code === 200) {
      transactions.value = data.data?.records || []
      total.value = Number(data.data?.total) || transactions.value.length
    }
  } finally { loading.value = false }
}

function openGrant() {
  grantForm.value = { userId: '', amount: 10, sourceId: '', remark: '' }
  grantVisible.value = true
}

async function doGrant() {
  if (!grantForm.value.userId || !grantForm.value.amount || !grantForm.value.sourceId) {
    ElMessage.warning('请填写用户ID、积分数量和来源标识')
    return
  }
  granting.value = true
  try {
    const { data } = await axios.post('/api/v1/admin/member/points/grant', grantForm.value)
    if (data.code === 200) {
      ElMessage.success('发放成功')
      grantVisible.value = false
      fetchData()
    } else {
      ElMessage.error(data.message || '发放失败')
    }
  } finally { granting.value = false }
}

fetchData()
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.toolbar-right { display: flex; gap: 12px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.filter-row { display: flex; gap: 12px; margin-bottom: 0; }
.font-mono { font-family: var(--font-mono); font-size: 13px; }
.value-text { font-size: 14px; color: var(--text-primary); }
.id-text { font-size: 12px; color: var(--text-muted); }
.time-text { font-size: 12px; color: var(--text-secondary); }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
