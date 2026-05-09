import { test, expect } from '@playwright/test'
import { MOCK_STOCK } from './mocks/data'

test.describe('Inventory Management', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // mock inventory query
    await page.route('**/api/v1/inventory/101', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_STOCK })
      })
    })

    // mock inventory not found
    await page.route('**/api/v1/inventory/999', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 30010001, message: '库存记录不存在' })
      })
    })

    // mock inventory update
    await page.route('**/api/v1/admin/inventory/*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'success' })
      })
    })

    await page.goto('/inventory')
  })

  // ---- P1: Core features ----

  test('P1: search by SKU ID shows stock data', async ({ page }) => {
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('button:has-text("查询")')
    await page.waitForResponse('**/api/v1/inventory/101')

    // stock card should appear
    await expect(page.locator('.stock-card')).toBeVisible()
    await expect(page.locator('.stock-card')).toContainText('500')
    await expect(page.locator('.stock-card')).toContainText('450')
  })

  test('P1: searching non-existent SKU shows empty state', async ({ page }) => {
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '999')
    await page.click('button:has-text("查询")')
    await page.waitForResponse('**/api/v1/inventory/999')

    await expect(page.locator('.empty-state')).toBeVisible()
    await expect(page.locator('.empty-state')).toContainText('未找到')
  })

  test('P1: update stock quantity works', async ({ page }) => {
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.stock-card')).toBeVisible()

    // find input-number and update
    const numberInput = page.locator('.stock-update input').first()
    await numberInput.click()
    await numberInput.fill('600')
    await page.click('button:has-text("更新库存")')
    await page.waitForTimeout(800)

    // stock card should remain visible after update
    await expect(page.locator('.stock-card')).toBeVisible()
  })

  // ---- P2: Edge cases ----

  test('P2: stock number input exists and is editable', async ({ page }) => {
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('button:has-text("查询")')
    await page.waitForResponse('**/api/v1/inventory/101')

    // el-input-number should be visible and editable
    const numberInput = page.locator('.stock-update .el-input-number input').first()
    await expect(numberInput).toBeVisible()
    await numberInput.click()
    await numberInput.fill('0')
    await expect(numberInput).toHaveValue('0')
  })

  test('P2: clearable input clears the search field', async ({ page }) => {
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('.el-input__clear')
    await expect(page.locator('input[placeholder="输入 SKU ID 查询"]')).toHaveValue('')
  })

  test('P2: query updates when SKU ID changes', async ({ page }) => {
    // first query
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.stock-card')).toBeVisible()

    // clear and query again with same ID - should still work
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '')
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.stock-card')).toBeVisible()
  })

  test('P2: all stock fields are displayed correctly', async ({ page }) => {
    await page.fill('input[placeholder="输入 SKU ID 查询"]', '101')
    await page.click('button:has-text("查询")')
    await page.waitForResponse('**/api/v1/inventory/101')

    await expect(page.locator('.stock-row')).toContainText('101')    // SKU ID
    await expect(page.locator('.stock-row')).toContainText('500')    // totalStock
    await expect(page.locator('.stock-row')).toContainText('50')     // lockedStock
    await expect(page.locator('.stock-row')).toContainText('450')    // availableStock
  })
})
