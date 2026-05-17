import { test, expect } from '@playwright/test'

test('merchant settlement page should use merchant settlement endpoint', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  let merchantEndpointHit = false
  await page.route('**/api/v1/admin/merchant/settlement*', async (route) => {
    merchantEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })

  await page.route('**/api/v1/admin/settlements*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })

  await page.goto('/merchant/settlement')

  await expect.poll(() => merchantEndpointHit).toBeTruthy()
  await expect(page.locator('button:has-text("生成今日结算")')).toHaveCount(0)
  await expect(page.getByText('暂无本店结算记录')).toBeVisible()
})
