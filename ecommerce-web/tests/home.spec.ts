import { test, expect } from '@playwright/test'
import { MOCK_PRODUCTS } from './mocks/data'

test.describe('PC Home', () => {

  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/products*', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCTS })
      })
    })
    await page.goto('/')
  })

  test('P0: home page loads with product grid', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('品质好物')
  })

  test('P0: search box exists and works', async ({ page }) => {
    await page.fill('input[placeholder="搜索商品..."]', 'iPhone')
    await page.click('button:has-text("搜索")')
    await page.waitForURL('**/products?keyword=iPhone**')
  })

  test('P1: product cards are clickable', async ({ page }) => {
    const card = page.locator('a[href^="/products/"]').first()
    await expect(card).toBeVisible()
    await card.click()
    await page.waitForURL('**/products/*')
  })

  test('P1: "查看全部" link navigates to products', async ({ page }) => {
    await page.click('a:has-text("查看全部")')
    await page.waitForURL('**/products')
  })
})
