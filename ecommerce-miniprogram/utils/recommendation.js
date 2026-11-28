import { request } from './api'
import { token } from './session'

export async function loadRecommendations() {
  const mode = token.value ? 'me' : 'popular'
  const result = await request({
    url: `/api/v1/recommendations/${mode}?limit=8&surface=MINI_PROGRAM`,
    timeout: 2800
  })
  return result.code === 200 ? result.data : null
}

export function recordRecommendationEvents(events) {
  if (token.value && events.length) {
    void request({ url: '/api/v1/recommendations/events', method: 'POST', data: events, timeout: 4000 })
  }
}
