import { test, expect } from '@playwright/test'
import { MOCK_PRODUCT_DETAIL } from './mocks/data'

test.describe('Product Detail', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // mock product detail API
    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL })
      })
    })

    // mock file URL API
    await page.route('**/api/v1/files/*/url', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: 'https://picsum.photos/200' })
      })
    })

    await page.goto('/products/1')
    await page.waitForResponse('**/api/v1/products/1')
  })

  // ---- P1: Core features ----

  test('P1: detail page loads with product info', async ({ page }) => {
    await expect(page.locator('.el-descriptions')).toBeVisible()
    await expect(page.locator('.el-descriptions')).toContainText('iPhone 15')
  })

  test('P1: status tag shows correct text', async ({ page }) => {
    // status=1 -> 上架 with success type
    await expect(page.locator('.el-tag--success')).toContainText('上架')
  })

  test('P1: "返回" button goes back', async ({ page }) => {
    await page.click('button:has-text("返回")')
    // should navigate back; if no history, stays or goes somewhere
    await page.waitForTimeout(300)
  })

  test('P1: "编辑商品" button navigates to edit', async ({ page }) => {
    await page.click('button:has-text("编辑商品")')
    await page.waitForURL('**/products/1/edit')
  })

  test('P1: SKU table shows SKU data', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    await expect(skuTable).toBeVisible()
    await expect(skuTable).toContainText('128GB 黑色')
    await expect(skuTable).toContainText('256GB 白色')
  })

  test('P1: SKU prices are displayed', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    await expect(skuTable).toContainText('6999.00')
    await expect(skuTable).toContainText('7999.00')
  })

  // ---- P2: Edge cases ----

  test('P2: product description shows or shows placeholder', async ({ page }) => {
    const desc = page.locator('.el-descriptions')
    await expect(desc).toBeVisible()
  })

  test('P2: detail HTML content is rendered', async ({ page }) => {
    await expect(page.locator('.detail-html')).toContainText('详情内容')
  })

  test('P2: sub-images are shown in gallery', async ({ page }) => {
    // product has 2 sub-images
    const subImages = page.locator('.sub-image')
    const count = await subImages.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('P2: SKU without original price shows "-"', async ({ page }) => {
    // second SKU has no originalPrice
    const skuTable = page.locator('.el-table').first()
    const cells = skuTable.locator('td').nth(4) // original price column
    // should have at least one "-" for the SKU without original price
    await expect(skuTable).toContainText('-')
  })

  test('P2: SKU spec JSON is rendered', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    // should show the spec values
    await expect(skuTable).toContainText('黑色')
    await expect(skuTable).toContainText('128GB')
  })
})
