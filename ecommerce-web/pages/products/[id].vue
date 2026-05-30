<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div v-if="loading" class="animate-pulse space-y-4">
      <div class="h-8 bg-gray-200 rounded w-1/3" />
      <div class="h-96 bg-gray-200 rounded-2xl" />
    </div>

    <div v-else-if="product" class="grid md:grid-cols-2 gap-12">
      <!-- Image Gallery -->
      <div>
        <div class="aspect-square bg-gray-50 rounded-2xl overflow-hidden border border-gray-100">
          <img :src="mainImage" :alt="product.spu.name" class="w-full h-full object-cover" />
        </div>
        <!-- Sub images -->
        <div v-if="subImages.length > 0" class="flex gap-2 mt-3">
          <img v-for="(img, i) in subImages" :key="i" :src="img" class="w-16 h-16 object-cover rounded-lg border border-gray-100" />
        </div>
      </div>

      <!-- Info -->
      <div>
        <h1 class="text-3xl font-bold text-gray-900">{{ product.spu.name }}</h1>
        <p class="mt-2 text-gray-500">{{ product.spu.description }}</p>
        <div class="mt-4 flex items-center gap-3">
          <span class="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded-full font-medium">{{ product.spu.status === 1 ? '在售' : '下架' }}</span>
          <span class="text-sm text-gray-400">{{ product.spu.reviewCount || 0 }} 条评价</span>
          <span v-if="product.spu.avgRating" class="text-sm text-amber-500 font-medium">★ {{ product.spu.avgRating }}</span>
        </div>

        <!-- SKU Selector -->
        <div class="mt-8 space-y-4">
          <h3 class="font-medium text-gray-900">选择规格</h3>
          <div class="grid grid-cols-2 gap-3">
            <button
              v-for="sku in product.skus"
              :key="sku.id"
              @click="selectedSku = sku"
              :class="[
                'text-left p-4 rounded-xl border-2 transition-all',
                selectedSku?.id === sku.id ? 'border-amber-400 bg-amber-50' : 'border-gray-100 hover:border-gray-200'
              ]"
            >
              <div class="font-medium text-gray-900 text-sm">{{ sku.name }}</div>
              <!-- Spec tags: parsed JSON → readable -->
              <div v-if="parseSpec(sku.spec).length > 0" class="flex flex-wrap gap-1.5 mt-2">
                <span v-for="(v, k) in parseSpec(sku.spec)" :key="k" class="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-md">
                  <span class="text-gray-400">{{ k }}</span>
                  <span class="ml-1 text-gray-700 font-medium">{{ v }}</span>
                </span>
              </div>
              <div class="mt-2 text-xl font-bold text-amber-600">¥{{ sku.price }}</div>
              <div v-if="sku.originalPrice" class="text-xs text-gray-400 line-through">¥{{ sku.originalPrice }}</div>
            </button>
          </div>
        </div>

        <!-- Selected SKU Detail -->
        <div v-if="selectedSku" class="mt-6 p-4 bg-amber-50 rounded-xl border border-amber-200">
          <div class="text-sm font-medium text-gray-700 mb-2">已选规格</div>
          <div class="flex flex-wrap gap-2">
            <span v-for="(v, k) in parseSpec(selectedSku.spec)" :key="k" class="text-sm">
              <span class="text-gray-400">{{ k }}:</span>
              <span class="text-gray-900 font-medium ml-1">{{ v }}</span>
            </span>
            <span v-if="parseSpec(selectedSku.spec).length === 0" class="text-gray-400 text-sm">默认规格</span>
          </div>
        </div>

        <!-- Add to Cart -->
        <div class="mt-8 flex gap-4">
          <button @click="addToCart" :disabled="!selectedSku" class="flex-1 h-14 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 disabled:text-gray-400 text-white font-medium rounded-xl transition-colors">
            {{ selectedSku ? '加入购物车' : '请选择规格' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const loading = ref(true)
const product = ref<any>(null)
const selectedSku = ref<any>(null)
const cart = useCartStore()
const { useResolvedImage, primeImageUrls, resolveImageUrl } = useImageUrl()
const mainImage = useResolvedImage(() => product.value?.spu?.mainImage)
const subImages = ref<string[]>([])

function parseSpec(spec: string): Record<string, string> {
  if (!spec) return {}
  try { return JSON.parse(spec) }
  catch { return {} }
}

onMounted(async () => {
  try {
    const api = useApi()
    const res: any = await api.get(`/products/${route.params.id}`)
    if (res.code === 200) {
      product.value = res.data

      let rawSubImages: string[] = []
      try { rawSubImages = JSON.parse(res.data?.spu?.images || '[]') } catch { rawSubImages = [] }
      // 详情页首屏会同时展示主图、附图、SKU 图，先统一解析成真实访问地址。
      await primeImageUrls([
        res.data?.spu?.mainImage,
        ...rawSubImages,
        ...(res.data?.skus || []).map((sku: any) => sku.image),
      ])
      subImages.value = await Promise.all(rawSubImages.map((img) => resolveImageUrl(img)))
    }
  } finally { loading.value = false }
})

async function addToCart() {
  if (!selectedSku.value) return
  await cart.addItem({
    skuId: selectedSku.value.id,
    spuId: product.value.spu.id,
    name: `${product.value.spu.name} - ${selectedSku.value.name}`,
    image: selectedSku.value.image || product.value.spu.mainImage,
    price: selectedSku.value.price,
    quantity: 1,
  })
}
</script>
