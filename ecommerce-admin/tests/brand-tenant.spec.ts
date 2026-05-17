import { test, expect } from '@playwright/test'

test('merchant brand page should use merchant brand endpoint and show merchant empty state', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('type', 'merchant')
    localStorage.setItem('username', 'merchant-a')
  })

  let merchantEndpointHit = false
  let platformEndpointHit = false

  await page.route('**/api/v1/admin/merchant/brands*', async (route) => {
    merchantEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })

  await page.route('**/api/v1/admin/brands*', async (route) => {
    platformEndpointHit = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })

  await page.goto('/merchant/brands')

  await expect.poll(() => merchantEndpointHit).toBeTruthy()
  expect(platformEndpointHit).toBeFalsy()
  await expect(page.getByRole('button', { name: '申请品牌' })).toBeVisible()
  await expect(page.getByText('暂无自有品牌，可先申请品牌')).toBeVisible()
})
