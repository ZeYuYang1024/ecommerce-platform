<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-8">商品分类</h1>

    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-4 gap-6">
      <div v-for="i in 8" :key="i" class="h-24 bg-gray-100 rounded-2xl animate-pulse" />
    </div>

    <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-6">
      <NuxtLink
        v-for="cat in categories"
        :key="cat.id"
        :to="`/products?categoryId=${cat.id}`"
        class="group bg-white rounded-2xl border border-gray-100 p-6 hover:shadow-md hover:border-amber-200 transition-all"
      >
        <h3 class="font-semibold text-gray-900 group-hover:text-amber-600 transition-colors">{{ cat.name }}</h3>
        <div v-if="cat.children && cat.children.length > 0" class="mt-3 flex flex-wrap gap-1.5">
          <span v-for="child in cat.children" :key="child.id"
            class="text-xs bg-gray-50 text-gray-500 px-2 py-1 rounded-md group-hover:bg-amber-50 group-hover:text-amber-600 transition-colors">
            {{ child.name }}
          </span>
        </div>
        <p v-else class="mt-2 text-xs text-gray-400">暂无子分类</p>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
const categories = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const api = useApi()
    const res: any = await api.get('/categories')
    if (res.code === 200) categories.value = res.data || []
  } finally { loading.value = false }
})
</script>
