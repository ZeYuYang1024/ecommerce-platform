type RecommendationEvent = {
  type: 'VIEW' | 'IMPRESSION' | 'CLICK'
  spuId: string
  requestId?: string
}

type RecommendationCard = {
  id: string
  name: string
  description?: string
  mainImage?: string
  minPrice?: number
  maxPrice?: number
  reviewCount?: number
}

export type RecommendationFeed = {
  requestId?: string
  personalized: boolean
  items: RecommendationCard[]
}

export function useRecommendation() {
  const auth = useAuthStore()
  const api = useApi()

  async function load(signal: AbortSignal) {
    const mode = auth.isLoggedIn ? 'me' : 'popular'
    const res = await api.get<{ code: number, data?: RecommendationFeed }>(
      `/recommendations/${mode}`, { limit: 12 }, { signal },
    )
    return res.code === 200 ? res.data : undefined
  }

  function record(events: RecommendationEvent[]) {
    if (auth.isLoggedIn && events.length) {
      void api.post('/recommendations/events', events).catch(() => {})
    }
  }

  return { load, record }
}
