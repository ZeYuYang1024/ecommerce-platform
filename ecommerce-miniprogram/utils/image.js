import { API_BASE } from './api'

export function getImageUrl(imageValue) {
  if (!imageValue) return '/static/product_01.png'
  if (imageValue.startsWith('http')) return imageValue
  if (imageValue.startsWith('/')) return imageValue
  return `${API_BASE}/api/v1/files/${imageValue}/url`
}
