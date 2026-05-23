import { test, expect } from '@playwright/test'

function sse(event: string, data: unknown) {
  return `event:${event}\ndata:${typeof data === 'string' ? data : JSON.stringify(data)}\n\n`
}

test('knowledge page should render streaming chunks from chat stream endpoint', async ({ page }) => {
  await page.context().addCookies([
    {
      name: 'token',
      value: 'mock-token',
      domain: 'localhost',
      path: '/',
    },
  ])

  let streamEndpointHit = false

  await page.route('**/api/v1/knowledge/chat/stream', async (route) => {
    streamEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream; charset=utf-8',
      body: [
        sse('start', 'stream-session'),
        sse('lightRoute', 'RAG_FAQ_CHANNEL'),
        sse('route', 'RAG_FAQ'),
        sse('chunk', '第一段'),
        sse('chunk', '第二段'),
        sse('answer', { answer: '第一段第二段', sessionId: 'stream-session', sources: [] }),
        sse('done', 'stream-session'),
      ].join(''),
    })
  })

  await page.goto('/knowledge')
  await page.getByPlaceholder('输入您的问题...').fill('平台退货规则是什么？')
  await page.getByRole('button', { name: '发送' }).click()

  await expect.poll(() => streamEndpointHit).toBeTruthy()
  await expect(page.getByText('第一段第二段')).toBeVisible()
})

test('knowledge page should show login hint when chat stream returns 401', async ({ page }) => {
  await page.route('**/api/v1/knowledge/chat/stream', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ code: 401, message: 'Unauthorized' }),
    })
  })

  await page.goto('/knowledge')
  await page.getByPlaceholder('输入您的问题...').fill('帮我查订单')
  await page.getByRole('button', { name: '发送' }).click()

  await expect(page.getByText('请先登录后再使用智能客服。')).toBeVisible()
})

test('knowledge page should show light route and route only in debug mode', async ({ page }) => {
  await page.context().addCookies([
    {
      name: 'token',
      value: 'mock-token',
      domain: 'localhost',
      path: '/',
    },
  ])

  await page.addInitScript(() => {
    localStorage.setItem('kb_debug', '1')
  })

  await page.route('**/api/v1/knowledge/chat/stream', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/event-stream; charset=utf-8',
      body: [
        sse('start', 'stream-session'),
        sse('lightRoute', 'RAG_FAQ_CHANNEL'),
        sse('route', 'RAG_FAQ'),
        sse('answer', { answer: '调试答案', sessionId: 'stream-session', sources: [] }),
        sse('done', 'stream-session'),
      ].join(''),
    })
  })

  await page.goto('/knowledge')
  await page.getByPlaceholder('输入您的问题...').fill('平台退货规则是什么？')
  await page.getByRole('button', { name: '发送' }).click()

  await expect(page.getByText('调试信息')).toBeVisible()
  await expect(page.getByText('轻量路由')).toBeVisible()
  await expect(page.getByText('RAG_FAQ_CHANNEL', { exact: true })).toBeVisible()
  await expect(page.getByText('业务路由')).toBeVisible()
  await expect(page.getByText('RAG_FAQ', { exact: true })).toBeVisible()
})
