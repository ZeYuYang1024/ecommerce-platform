<template>
  <header class="bg-white border-b border-gray-200 sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
      <div class="flex items-center gap-8">
        <NuxtLink to="/" class="text-xl font-bold tracking-tight">
          <span class="text-gray-900">MERCH</span><span class="text-amber-600">.</span>
        </NuxtLink>
        <nav class="hidden md:flex items-center gap-6 text-sm">
          <NuxtLink to="/products" class="text-gray-600 hover:text-gray-900 transition-colors">全部商品</NuxtLink>
          <NuxtLink to="/categories" class="text-gray-600 hover:text-gray-900 transition-colors">分类</NuxtLink>
          <NuxtLink to="/knowledge" class="text-gray-600 hover:text-amber-600 transition-colors flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/></svg>
            智能客服
          </NuxtLink>
        </nav>
      </div>

      <div class="flex items-center gap-4">
        <NuxtLink to="/cart" class="relative p-2 text-gray-600 hover:text-gray-900 transition-colors">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z"/></svg>
          <span v-if="cart.count" class="absolute -top-1 -right-1 bg-amber-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-medium">{{ cart.count }}</span>
        </NuxtLink>

        <template v-if="auth.isLoggedIn">
          <NuxtLink to="/user" class="text-sm text-gray-600 hover:text-gray-900">{{ auth.username }}</NuxtLink>
          <button @click="auth.logout()" class="text-sm text-gray-400 hover:text-gray-600">退出</button>
        </template>
        <template v-else>
          <NuxtLink to="/login" class="text-sm text-gray-600 hover:text-gray-900">登录</NuxtLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const cart = useCartStore()
onMounted(() => cart.fetchCart())
</script>
