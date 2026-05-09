<template>
  <NuxtLink :to="`/products/${product.id}`" class="group bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-lg hover:border-gray-200 transition-all duration-300">
    <div class="aspect-square bg-gray-50 overflow-hidden">
      <img
        :src="product.mainImage || '/placeholder.svg'"
        :alt="product.name"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
      />
    </div>
    <div class="p-4">
      <h3 class="font-medium text-gray-900 truncate">{{ product.name }}</h3>
      <p class="text-sm text-gray-400 mt-1 truncate">{{ product.description || '暂无描述' }}</p>
      <div class="flex items-center justify-between mt-3">
        <span class="text-lg font-bold text-amber-600">--</span>
        <span class="text-xs text-gray-400">{{ product.reviewCount || 0 }} 评价</span>
      </div>
    </div>
  </NuxtLink>
</template>

<script setup lang="ts">
defineProps<{ product: any }>()
</script>
