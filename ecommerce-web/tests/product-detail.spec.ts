import { test, expect } from '@playwright/test'
import { MOCK_PRODUCT_DETAIL } from './mocks/data'

test.describe('PC Product Detail', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/files/*/url', async (route) => {
      const path = new URL(route.request().url()).pathname
      const objectName = path.split('/').at(-2)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: `https://cdn.test/${objectName}` })
      })
    })

    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL })
      })
    })

    await page.goto('/products/1')
    await page.waitForTimeout(800)
  })

  test('P0: product detail loads with SKU options', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('iPhone')
    await expect(page.locator('button:has-text("128GB")')).toBeVisible()
  })

  test('P0: product detail resolves object-name images through file service', async ({ page }) => {
    const mainImage = page.locator('img[alt="iPhone 15 Pro"]').first()
    await expect(mainImage).toHaveAttribute('src', /https:\/\/cdn\.test\/iphone\.jpg/)
  })

  test('P0: add to cart button disabled without SKU', async ({ page }) => {
    await expect(page.locator('button').last()).toBeDisabled()
  })

  test('P0: select SKU enables add to cart', async ({ page }) => {
    await page.locator('button:has-text("128GB")').click()
    await expect(page.locator('button').last()).toBeEnabled()
  })

  test('P0: stale login should be cleared when cart prefetch returns 401', async ({ page, context }) => {
    await context.addCookies([
      { name: 'token', value: 'expired-token', domain: 'localhost', path: '/' },
      { name: 'username', value: 'zhangsan', domain: 'localhost', path: '/' }
    ])
    await page.route('**/api/v1/cart', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ code: 401, message: 'Unauthorized' })
      })
    })

    await page.goto('/products/1')
    await page.waitForTimeout(800)

    await expect(page.locator('text=zhangsan')).toHaveCount(0)
    const tokenCookie = (await context.cookies()).find((cookie) => cookie.name === 'token')
    expect(tokenCookie).toBeFalsy()
  })
})
