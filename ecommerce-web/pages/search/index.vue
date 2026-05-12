<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="flex flex-wrap items-center gap-3 mb-8">
      <input v-model="keyword" @keyup.enter="search" placeholder="搜索商品..." class="flex-1 min-w-[200px] h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-amber-300" />
      <select v-model="categoryId" @change="search" class="h-11 px-4 rounded-xl border border-gray-200 text-sm bg-white">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <select v-model="sort" @change="search" class="h-11 px-4 rounded-xl border border-gray-200 text-sm bg-white">
        <option value="">综合排序</option>
        <option value="price_asc">价格从低到高</option>
        <option value="price_desc">价格从高到低</option>
        <option value="rating">评分最高</option>
        <option value="sales">销量最高</option>
      </select>
    </div>

    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-4 gap-6">
      <div v-for="i in 8" :key="i" class="bg-white rounded-2xl border border-gray-100 aspect-square animate-pulse" />
    </div>

    <div v-else-if="products.length === 0" class="text-center py-16 text-gray-400">
      未找到相关商品
      <NuxtLink to="/products" class="text-amber-600 block mt-2">浏览全部商品 →</NuxtLink>
    </div>

    <div v-else>
      <p class="text-sm text-gray-400 mb-4">找到 {{ total }} 件商品</p>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <ProductCard v-for="p in products" :key="p.id" :product="p" />
      </div>

      <div v-if="total > size" class="mt-8 flex justify-center gap-2">
        <button v-for="p in Math.min(Math.ceil(total / size), 10)" :key="p" @click="page = p; fetchData()" :class="['w-10 h-10 rounded-lg font-medium text-sm transition-colors', page === p ? 'bg-amber-500 text-white' : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50']">{{ p }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const keyword = ref((route.query.keyword as string) || '')
const categoryId = ref((route.query.categoryId as string) || '')
const sort = ref((route.query.sort as string) || '')
const categories = ref<any[]>([])
const products = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const size = ref(12)
const total = ref(0)

onMounted(async () => {
  try {
    const api = useApi()
    const catRes: any = await api.get('/categories')
    if (catRes.code === 200) categories.value = catRes.data || []
  } catch {}
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const api = useApi()
    const params = new URLSearchParams({ page: String(page.value), size: String(size.value) })
    if (keyword.value) params.set('keyword', keyword.value)
    if (categoryId.value) params.set('categoryId', categoryId.value)
    if (sort.value) params.set('sort', sort.value)
    const res: any = await api.get(`/search?${params}`)
    if (res.code === 200) {
      products.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally { loading.value = false }
}

function search() {
  page.value = 1
  fetchData()
}
</script>
