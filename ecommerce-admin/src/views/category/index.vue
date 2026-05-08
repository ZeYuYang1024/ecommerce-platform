<template>
  <div>
    <div class="toolbar">
      <span class="section-label">分类管理</span>
      <el-button type="primary" @click="showDialog(null)">
        <el-icon style="margin-right:4px"><Plus /></el-icon> 新增分类
      </el-button>
    </div>

    <el-card v-if="categories.length > 0">
      <el-tree
        :data="categories"
        :props="{ children: 'children', label: 'name' }"
        node-key="id"
        default-expand-all
        highlight-current
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span class="tree-label">
              <span v-if="data.level === 1" class="level-badge l1">一级</span>
              <span v-else class="level-badge l2">二级</span>
              {{ data.name }}
            </span>
            <span class="tree-actions">
              <el-button size="small" @click.stop="showDialog(data)">编辑</el-button>
              <el-button size="small" @click.stop="showDialog({ parentId: data.id })">
                <el-icon><Plus /></el-icon> 子分类
              </el-button>
              <el-popconfirm title="确认删除？子分类也会被删除" @confirm="handleDelete(data.id)">
                <template #reference><el-button size="small" class="btn-danger" @click.stop>删除</el-button></template>
              </el-popconfirm>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>
    <el-empty v-else description="暂无分类" />

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-position="top">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="allCategories"
            :props="{ children: 'children', label: 'name', value: 'id' }"
            placeholder="不选则为一级分类"
            clearable
            check-strictly
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'

const categories = ref([])
const allCategories = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', parentId: null, sort: 0 })

const dialogTitle = computed(() => {
  if (editingId.value) return '编辑分类'
  if (form.parentId) return '新增子分类'
  return '新增分类'
})

async function fetchData() {
  const { data } = await axios.get('/api/v1/admin/categories')
  if (data.code === 200) allCategories.value = data.data

  const { data: treeData } = await axios.get('/api/v1/categories')
  if (treeData.code === 200) categories.value = treeData.data
}

function showDialog(row) {
  if (row && row.id) {
    editingId.value = row.id
    form.name = row.name
    form.parentId = row.parentId || null
    form.sort = row.sort
  } else {
    editingId.value = null
    form.name = ''
    form.parentId = row ? row.parentId || null : null
    form.sort = 0
  }
  dialogVisible.value = true
}

async function submit() {
  const payload = {
    name: form.name,
    parentId: form.parentId || 0,
    sort: form.sort
  }
  if (editingId.value) {
    await axios.put(`/api/v1/admin/categories/${editingId.value}`, payload)
  } else {
    await axios.post('/api/v1/admin/categories', payload)
  }
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(id) {
  await axios.delete(`/api/v1/admin/categories/${id}`)
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-label { font-size: 15px; font-weight: 600; color: var(--text-primary); }

.tree-node { flex: 1; display: flex; align-items: center; justify-content: space-between; padding: 4px 0; }
.tree-label { display: flex; align-items: center; gap: 8px; font-weight: 500; }
.level-badge {
  font-size: 10px; font-weight: 700; padding: 2px 6px; border: 1px solid;
  text-transform: uppercase; letter-spacing: 0.08em;
}
.level-badge.l1 { color: var(--text-primary); border-color: var(--border-default); background: var(--bg-hover); }
.level-badge.l2 { color: var(--text-muted); border-color: var(--border-subtle); }
.tree-actions { display: flex; gap: 4px; flex-shrink: 0; }

.btn-danger {
  --el-button-text-color: var(--red);
  --el-button-hover-text-color: #fff;
  --el-button-hover-bg-color: var(--red);
  --el-button-hover-border-color: var(--red);
}
</style>
