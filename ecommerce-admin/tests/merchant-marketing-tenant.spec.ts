import { expect, test } from '@playwright/test'

test.describe('Merchant marketing tenant routes', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'merchant-a')
      localStorage.setItem('type', 'merchant')
      localStorage.setItem('merchantId', '2001')
    })
  })

  test('merchant coupon page should use merchant coupon endpoint', async ({ page }) => {
    let merchantCouponsHit = 0
    let platformCouponsHit = 0

    await page.route('**/api/v1/admin/merchant/coupons*', async (route) => {
      merchantCouponsHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.route('**/api/v1/admin/coupons*', async (route) => {
      platformCouponsHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.goto('/merchant/coupons')
    await page.waitForTimeout(300)

    expect(merchantCouponsHit).toBeGreaterThan(0)
    expect(platformCouponsHit).toBe(0)
  })

  test('merchant seckill page should use merchant seckill endpoints', async ({ page }) => {
    let merchantSessionsHit = 0
    let merchantItemsHit = 0
    let platformSessionsHit = 0
    let platformItemsHit = 0

    await page.route('**/api/v1/admin/merchant/seckill/sessions*', async (route) => {
      merchantSessionsHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.route('**/api/v1/admin/merchant/seckill/items*', async (route) => {
      merchantItemsHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.route('**/api/v1/admin/seckill/sessions*', async (route) => {
      platformSessionsHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.route('**/api/v1/admin/seckill/items*', async (route) => {
      platformItemsHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: { records: [], total: 0, current: 1, size: 10 } })
      })
    })

    await page.goto('/merchant/seckill')
    await page.waitForTimeout(300)

    expect(merchantSessionsHit).toBeGreaterThan(0)
    expect(merchantItemsHit).toBeGreaterThan(0)
    expect(platformSessionsHit).toBe(0)
    expect(platformItemsHit).toBe(0)
  })
})
