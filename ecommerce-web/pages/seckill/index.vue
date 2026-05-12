<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-8">限时秒杀</h1>

    <div v-if="loading" class="text-center py-8 text-gray-400">加载中...</div>

    <div v-else-if="sessions.length===0" class="text-center py-16 text-gray-400">
      暂无秒杀活动
      <NuxtLink to="/products" class="text-amber-600 block mt-2">逛逛商品 →</NuxtLink>
    </div>

    <div v-else v-for="s in sessions" :key="s.id" class="mb-12">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-semibold text-gray-800">{{ s.name }}</h2>
        <span class="text-sm text-red-500 font-mono">距结束 {{ countdown(s.endTime) }}</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div v-for="item in itemsBySession[s.id]" :key="item.id" class="bg-white rounded-2xl border border-red-100 p-4 hover:shadow-lg transition-shadow">
          <div class="text-sm font-medium truncate mb-2">{{ item.name }}</div>
          <div class="flex items-baseline gap-2 mb-3">
            <span class="text-xl font-bold text-red-500">¥{{ item.seckillPrice }}</span>
            <span class="text-xs text-gray-400 line-through">¥{{ item.originalPrice }}</span>
          </div>
          <div class="text-xs text-gray-400 mb-3">剩余 {{ item.remainingCount }} 件</div>
          <button @click="buy(item)" class="w-full py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-medium rounded-lg transition-colors" :disabled="item.remainingCount<=0">
            {{ item.remainingCount>0?'立即秒杀':'已抢光' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const sessions = ref<any[]>([])
const itemsBySession = ref<Record<string,any[]>>({})
const loading = ref(true)

onMounted(async () => {
  try {
    const api = useApi()
    const [sRes,iRes] = await Promise.all([
      api.get('/seckill/sessions'),
      api.get('/seckill/items?sessionId=')
    ])
    if (sRes.code===200) sessions.value = sRes.data || []
    if (iRes.code===200) {
      const map: Record<string,any[]> = {}
      for (const item of iRes.data||[]) {
        const sid = String(item.sessionId)
        if (!map[sid]) map[sid] = []
        map[sid].push(item)
      }
      itemsBySession.value = map
    }
  } finally { loading.value = false }
})

function countdown(end: string) {
  const diff = new Date(end).getTime() - Date.now()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff/3600000), m = Math.floor((diff%3600000)/60000), s = Math.floor((diff%60000)/1000)
  return `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
}

async function buy(item: any) {
  const api = useApi()
  try {
    await api.post('/seckill/order', { itemId: item.id, userId: 1 })
    alert('抢购成功！')
    item.remainingCount--
  } catch { alert('抢购失败') }
}
</script>
