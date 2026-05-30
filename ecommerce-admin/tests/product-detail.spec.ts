import { test, expect } from '@playwright/test'
import { MOCK_PRODUCT_DETAIL } from './mocks/data'

async function gotoProductDetail(page) {
  const detailResponse = page.waitForResponse((response) => (
    response.request().method() === 'GET'
    && response.url().includes('/api/v1/products/1')
  ))

  await page.goto('/products/1')
  await detailResponse
}

test.describe('Product Detail', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL }),
      })
    })

    await page.route('**/api/v1/files/*/url', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: 'https://picsum.photos/200' }),
      })
    })

    await gotoProductDetail(page)
  })

  test('P1: detail page loads with product info', async ({ page }) => {
    await expect(page.locator('.detail-layout')).toBeVisible()
    await expect(page.locator('.product-title')).toContainText('iPhone 15')
  })

  test('P1: status tag shows success state for an active product', async ({ page }) => {
    await expect(page.locator('.el-tag--success')).toBeVisible()
  })

  test('P1: back button remains clickable', async ({ page }) => {
    await page.locator('.back-btn').click()
    await page.waitForTimeout(300)
  })

  test('P1: edit button navigates to edit page', async ({ page }) => {
    await page.locator('.detail-actions .el-button--primary').click()
    await page.waitForURL('**/products/1/edit')
  })

  test('P1: SKU table shows SKU data', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    await expect(skuTable).toBeVisible()
    await expect(skuTable).toContainText('128GB')
    await expect(skuTable).toContainText('256GB')
  })

  test('P1: SKU prices are displayed', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    await expect(skuTable).toContainText('6999.00')
    await expect(skuTable).toContainText('7999.00')
  })

  test('P2: product description block is rendered', async ({ page }) => {
    await expect(page.locator('.info-desc')).toBeVisible()
  })

  test('P2: detail HTML content is rendered', async ({ page }) => {
    await expect(page.locator('.detail-html')).toContainText('详情内容')
  })

  test('P2: sub-images are shown in the gallery', async ({ page }) => {
    const subImages = page.locator('.thumb-img')
    const count = await subImages.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('P2: detail page resolves main image to real preview URL', async ({ page }) => {
    await expect(page.locator('.main-image')).toHaveAttribute('src', /picsum\.photos\/200/)
  })

  test('P2: SKU without original price shows placeholder', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    await expect(skuTable).toContainText('-')
  })

  test('P2: SKU spec JSON is rendered', async ({ page }) => {
    const skuTable = page.locator('.el-table').first()
    await expect(skuTable).toContainText('color')
    await expect(skuTable).toContainText('storage')
  })
})
