import { request } from './api'

const imageUrls = new Map()
const pending = new Map()
const cacheMs = 60 * 60 * 1000

export async function resolveImageUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return ''
  const source = value.trim()
  const resolver = source.match(/^\/api\/v1\/files\/([^/]+)\/url$/)
  if (!resolver && (/^(https?:)?\/\//.test(source) || source.startsWith('/'))) return source

  const key = resolver ? resolver[1] : encodeURIComponent(source)
  const cached = imageUrls.get(key)
  if (cached?.expiresAt > Date.now()) return cached.url

  if (!pending.has(key)) {
    pending.set(key, request({ url: `/api/v1/files/${key}/url`, auth: false }).then(res => {
      if (res.code !== 200 || typeof res.data !== 'string') return ''
      imageUrls.set(key, { url: res.data, expiresAt: Date.now() + cacheMs })
      return res.data
    }).finally(() => pending.delete(key)))
  }
  return pending.get(key)
}

export function productImages(product) {
  let gallery = product.images || []
  if (typeof gallery === 'string') {
    try {
      gallery = JSON.parse(gallery)
    } catch {
      gallery = gallery.split(',')
    }
  }
  const sources = [product.mainImage, ...(Array.isArray(gallery) ? gallery : [])]
  return [...new Set(sources.filter(value => typeof value === 'string' && value.trim()).map(value => value.trim()))]
}
