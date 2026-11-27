<template>
  <div>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-amber-50 to-white">
      <div class="max-w-7xl mx-auto px-4 py-20 text-center">
        <h1 class="text-4xl md:text-5xl font-bold text-gray-900 tracking-tight">品质好物 一站购齐</h1>
        <p class="mt-4 text-lg text-gray-500 max-w-xl mx-auto">精选优质商家，正品保障，极速物流</p>
        <div class="mt-8 max-w-lg mx-auto flex gap-2">
          <input v-model="keyword" @keyup.enter="search" placeholder="搜索商品..." class="flex-1 h-12 px-5 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-amber-300 focus:border-amber-400 text-sm" />
          <button @click="search" class="h-12 px-6 bg-amber-500 hover:bg-amber-600 text-white font-medium rounded-xl transition-colors">搜索</button>
        </div>
      </div>
    </section>

    <!-- Product Grid -->
    <section class="max-w-7xl mx-auto px-4 py-16">
      <div class="flex items-center justify-between mb-8">
        <h2 class="text-2xl font-bold text-gray-900">热门商品</h2>
        <NuxtLink to="/products" class="text-sm text-amber-600 hover:text-amber-700 font-medium">查看全部 →</NuxtLink>
      </div>
      <div v-if="loading" class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <div v-for="i in 4" :key="i" class="bg-white rounded-2xl border border-gray-100 aspect-square animate-pulse" />
      </div>
      <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <ProductCard v-for="p in products" :key="p.id" :product="p" />
      </div>
    </section>

    <section v-if="feed?.items.length" ref="recommendationSection" class="max-w-7xl mx-auto px-4 pb-16">
      <h2 class="mb-8 text-2xl font-bold text-gray-900">猜你喜欢</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <ProductCard
          v-for="p in feed.items"
          :key="p.id"
          :product="p"
          :data-recommendation-id="p.id"
          @click="recordClick(p.id)"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { RecommendationFeed } from '~/composables/useRecommendation'

const keyword = ref('')
const products = ref<any[]>([])
const loading = ref(true)
const router = useRouter()
const auth = useAuthStore()
const recommendation = useRecommendation()
const feed = ref<RecommendationFeed>()
const recommendationSection = ref<HTMLElement>()
let request: AbortController | undefined
let observer: IntersectionObserver | undefined

async function refreshRecommendations() {
  request?.abort()
  observer?.disconnect()
  feed.value = undefined
  const currentRequest = new AbortController()
  request = currentRequest
  const timeout = window.setTimeout(() => currentRequest.abort(), 2800)
  try {
    const result = await recommendation.load(currentRequest.signal)
    if (currentRequest.signal.aborted || request !== currentRequest || !result) return
    feed.value = result
    await nextTick()
    if (!result.requestId || request !== currentRequest || !recommendationSection.value) return
    const requestId = result.requestId
    observer = new IntersectionObserver((entries, activeObserver) => {
      if (request !== currentRequest) return
      const visible = entries.filter(entry => entry.isIntersecting && entry.intersectionRatio >= 0.5)
      recommendation.record(visible.flatMap(entry => {
        activeObserver.unobserve(entry.target)
        const spuId = (entry.target as HTMLElement).dataset.recommendationId
        return spuId ? [{ type: 'IMPRESSION', spuId, requestId }] : []
      }))
    }, { threshold: 0.5 })
    recommendationSection.value.querySelectorAll('[data-recommendation-id]').forEach(card => observer?.observe(card))
  } catch {
    // The regular product list remains available when recommendations time out.
  } finally {
    window.clearTimeout(timeout)
  }
}

function recordClick(spuId: string) {
  const requestId = feed.value?.requestId
  if (requestId) recommendation.record([{ type: 'IMPRESSION', requestId, spuId }, { type: 'CLICK', requestId, spuId }])
}

onMounted(async () => {
  watch(() => auth.token, refreshRecommendations, { immediate: true })
  try {
    const api = useApi()
    const res: any = await api.get('/products?page=1&size=12')
    if (res.code === 200) products.value = res.data?.records || []
  } finally { loading.value = false }
})

onBeforeUnmount(() => {
  request?.abort()
  observer?.disconnect()
})

function search() {
  if (keyword.value.trim()) {
    router.push(`/search?keyword=${encodeURIComponent(keyword.value.trim())}`)
  }
}
</script>
