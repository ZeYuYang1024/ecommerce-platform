<template>
  <div v-if="total > 0" class="pagination-root">
    <div class="pagination-row">
      <span class="total-text">共 {{ total }} 条</span>

      <select v-model="localSize" class="size-select" @change="onSizeChange">
        <option v-for="s in sizeOptions" :key="s" :value="s">{{ s }} 条/页</option>
      </select>

      <div class="page-btns">
        <button :disabled="localPage <= 1" class="page-btn arrow" @click="goPage(localPage - 1)">&lt;</button>

        <template v-for="p in displayPages" :key="p">
          <span v-if="p === -1" class="ellipsis">...</span>
          <button v-else :class="['page-btn', p === localPage ? 'active' : '']" @click="goPage(p)">{{ p }}</button>
        </template>

        <button :disabled="localPage >= totalPages" class="page-btn arrow" @click="goPage(localPage + 1)">&gt;</button>
      </div>

      <div class="jumper">
        <span class="jumper-label">跳至</span>
        <input v-model="jumpInput" class="jump-input" type="number" :min="1" :max="totalPages" @keyup.enter="doJump" />
        <span class="jumper-label">页</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ page: number; size: number; total: number }>()
const emit = defineEmits<{ 'update:page': [p: number]; 'update:size': [s: number]; change: [] }>()

const sizeOptions = [10, 20, 50, 100]

const localPage = ref(props.page)
const localSize = ref(props.size)
const jumpInput = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

const displayPages = computed(() => {
  const total = totalPages.value
  const cur = localPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages: number[] = [1]
  if (cur > 3) pages.push(-1)
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push(-1)
  pages.push(total)
  return pages
})

watch(() => props.page, v => { localPage.value = v })
watch(() => props.size, v => { localSize.value = v })

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  localPage.value = p
  emit('update:page', p)
  emit('change')
}

function onSizeChange() {
  emit('update:size', localSize.value)
  localPage.value = 1
  emit('update:page', 1)
  emit('change')
}

function doJump() {
  const n = parseInt(jumpInput.value)
  if (n >= 1 && n <= totalPages.value) {
    jumpInput.value = ''
    goPage(n)
  }
}
</script>

<style scoped>
.pagination-root { margin-top: 32px; }
.pagination-row {
  display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap;
}
.total-text { font-size: 13px; color: #6b7280; white-space: nowrap; }
.size-select {
  padding: 6px 10px; font-size: 13px; border: 1px solid #e5e7eb; border-radius: 8px;
  background: #fff; color: #374151; cursor: pointer; outline: none;
}
.size-select:focus { border-color: #f59e0b; }
.page-btns { display: flex; align-items: center; gap: 4px; }
.page-btn {
  min-width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: 1px solid #e5e7eb; border-radius: 8px; background: #fff;
  font-size: 13px; color: #374151; cursor: pointer; transition: all 0.15s;
}
.page-btn:hover:not(:disabled):not(.active) { background: #f9fafb; }
.page-btn.active { background: #f59e0b; color: #fff; border-color: #f59e0b; font-weight: 600; }
.page-btn.arrow { font-weight: 700; }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.ellipsis { width: 36px; text-align: center; color: #9ca3af; font-size: 13px; }
.jumper { display: flex; align-items: center; gap: 4px; }
.jumper-label { font-size: 13px; color: #6b7280; white-space: nowrap; }
.jump-input {
  width: 48px; height: 36px; text-align: center; font-size: 13px;
  border: 1px solid #e5e7eb; border-radius: 8px; outline: none;
}
.jump-input:focus { border-color: #f59e0b; }
</style>
