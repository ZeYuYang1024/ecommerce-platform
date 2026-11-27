<template>
  <view class="product-media" :style="{ width, height, borderRadius: radius }">
    <image v-if="url" class="image" :src="url" :mode="mode" @error="url = ''" />
    <view v-else class="placeholder"><text>暂无图片</text></view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { resolveImageUrl } from '@/utils/image'

const props = defineProps({
  source: String,
  height: { type: String, default: '100%' },
  width: { type: String, default: '100%' },
  radius: String,
  mode: { type: String, default: 'aspectFill' }
})
const url = ref('')
watch(() => props.source, source => {
  url.value = resolveImageUrl(source)
}, { immediate: true })
</script>

<style scoped>
.product-media { flex-shrink: 0; overflow: hidden; background: #F1F3F5; }
.image { display: block; width: 100%; height: 100%; }
.placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #5F6B7A; font-size: 24rpx; }
</style>
