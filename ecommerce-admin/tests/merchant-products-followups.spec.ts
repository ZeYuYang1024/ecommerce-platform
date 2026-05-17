import { expect, test } from '@playwright/test'

test.describe('Merchant product follow-ups', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'merchant-a')
      localStorage.setItem('type', 'merchant')
      localStorage.setItem('merchantId', '2001')
    })

    await page.route('**/api/v1/categories', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: [] })
      })
    })
  })

  test('merchant products page should use merchant list endpoint and merchant create route', async ({ page }) => {
    let merchantListHit = 0
    let platformListHit = 0

    await page.route('**/api/v1/admin/merchant/products*', async (route) => {
      merchantListHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0, current: 1, size: 10 }
        })
      })
    })

    await page.route('**/api/v1/admin/products*', async (route) => {
      platformListHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: [], total: 0, current: 1, size: 10 }
        })
      })
    })

    await page.goto('/merchant/products')
    await expect(page.getByRole('button', { name: '新增商品' })).toBeVisible()

    expect(merchantListHit).toBeGreaterThan(0)
    expect(platformListHit).toBe(0)

    await page.getByRole('button', { name: '新增商品' }).click()
    await expect(page).toHaveURL(/\/merchant\/products\/create$/)
  })

  test('merchant create form should submit to merchant endpoint and return to merchant list', async ({ page }) => {
    let merchantCreateHit = 0
    let platformCreateHit = 0

    await page.route('**/api/v1/admin/merchant/products', async (route) => {
      merchantCreateHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: 101, name: 'merchant-created' }
        })
      })
    })

    await page.route('**/api/v1/admin/products', async (route) => {
      platformCreateHit += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { id: 101, name: 'platform-created' }
        })
      })
    })

    await page.goto('/merchant/products/create')
    await page.getByPlaceholder('请输入商品名称').fill('商家新商品')
    await page.getByRole('button', { name: '创建商品' }).click()

    expect(merchantCreateHit).toBeGreaterThan(0)
    expect(platformCreateHit).toBe(0)
    await expect(page).toHaveURL(/\/merchant\/products$/)
  })
})
