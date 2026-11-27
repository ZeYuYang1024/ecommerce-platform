import { API_BASE } from './api'

export function resolveImageUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return ''
  const source = value.trim()
  const resolver = source.match(/^\/api\/v1\/files\/([^/]+)\/(?:url|content)$/)
  if (!resolver && (/^(https?:)?\/\//.test(source) || source.startsWith('/'))) return source

  const key = resolver ? resolver[1] : encodeURIComponent(source)
  return `${API_BASE}/api/v1/files/${key}/content`
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
