<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">个人中心</h1>
    <div class="mt-8 bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
      <div class="flex items-center justify-between py-2">
        <span class="text-gray-500">用户名</span>
        <span class="font-medium">{{ auth.username }}</span>
      </div>
      <div class="border-t border-gray-50 pt-4">
        <NuxtLink to="/user/orders" class="flex items-center justify-between py-2 hover:text-amber-600 transition-colors">
          <span>我的订单</span>
          <span class="text-gray-300">→</span>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
</script>
