import { test, expect } from '@playwright/test'
import { MOCK_PRODUCT_DETAIL } from './mocks/data'

test.describe('PC Product Detail', () => {

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
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

  test('P0: add to cart button disabled without SKU', async ({ page }) => {
    await expect(page.locator('button:has-text("请选择规格")')).toBeDisabled()
  })

  test('P0: select SKU enables add to cart', async ({ page }) => {
    await page.locator('button:has-text("128GB")').click()
    await expect(page.locator('button:has-text("加入购物车")')).toBeEnabled()
  })
})
