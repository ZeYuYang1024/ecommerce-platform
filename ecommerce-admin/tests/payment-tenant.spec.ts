import { test, expect } from '@playwright/test'

test('merchant payments page should use merchant payment endpoint', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  let merchantEndpointHit = false
  await page.route('**/api/v1/admin/merchant/payment*', async (route) => {
    merchantEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })

  await page.route('**/api/v1/admin/payment*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })

  await page.goto('/merchant/payments')

  await expect.poll(() => merchantEndpointHit).toBeTruthy()
  await expect(page.getByText('暂无本店支付记录')).toBeVisible()
})
