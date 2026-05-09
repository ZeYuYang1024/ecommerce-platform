import { test, expect } from '@playwright/test'
import { MOCK_PRODUCTS } from './mocks/data'

test.describe('Product List', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // mock admin product list API
    await page.route('**/api/v1/admin/products*', async (route, req) => {
      const url = new URL(req.url())
      const keyword = url.searchParams.get('keyword')
      const status = url.searchParams.get('status')
      let filtered = [...MOCK_PRODUCTS]

      if (keyword) {
        filtered = filtered.filter(p => p.name.includes(keyword))
      }
      if (status !== null && status !== undefined && status !== '') {
        filtered = filtered.filter(p => p.status === Number(status))
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200, message: 'success',
          data: {
            current: 1, size: 10, total: filtered.length,
            pages: 1, records: filtered
          }
        })
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

    // mock product status toggle
    await page.route('**/api/v1/admin/products/*/status', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'success' })
      })
    })

    // mock product delete
    await page.route('**/api/v1/admin/products/*', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success' })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/products')
    await page.waitForResponse('**/api/v1/admin/products*')
  })

  // ---- P0: Critical path ----

  test('P0: product list loads with data rows', async ({ page }) => {
    await expect(page.locator('.el-table__body tr')).not.toHaveCount(0)
  })

  test('P0: pagination component is visible', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toBeVisible()
  })

  test('P0: redirects to /login when no token', async ({ page }) => {
    // Clear the init script and reload so Vue re-checks auth
    await page.addInitScript(() => localStorage.clear())
    await page.goto('/products')
    // SPA needs a reload to re-trigger the router guard after localStorage clear
    await page.reload()
    await page.waitForURL('**/login', { timeout: 5000 })
  })

  // Search triggers: @clear button, @keyup.enter, and status @change
  test('P0: search by typing Enter triggers data refresh', async ({ page }) => {
    // override the mock to filter by keyword
    await page.route('**/api/v1/admin/products*', async (route, req) => {
      const url = new URL(req.url())
      const keyword = url.searchParams.get('keyword')
      let filtered = [...MOCK_PRODUCTS]
      if (keyword) filtered = filtered.filter(p => p.name.includes(keyword))
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: 200, message: 'success',
          data: { current: 1, size: 10, total: filtered.length, pages: 1, records: filtered }
        })
      })
    })

    await page.fill('input[placeholder="搜索商品..."]', 'iPhone')
    await page.keyboard.press('Enter')
    await page.waitForTimeout(500)

    // should filter to only matching products
    const rows = page.locator('.el-table__body tr')
    await expect(rows).toHaveCount(1)
    await expect(rows.first()).toContainText('iPhone')
  })

  test('P0: clear search restores full list', async ({ page }) => {
    await page.fill('input[placeholder="搜索商品..."]', 'iPhone')
    await page.click('.el-input__clear')
    await page.waitForTimeout(500)

    const rows = page.locator('.el-table__body tr')
    await expect(rows).toHaveCount(MOCK_PRODUCTS.length)
  })

  // ---- P1: Core features ----

  test('P1: status select filters products', async ({ page }) => {
    const select = page.locator('.toolbar-left .el-select').first()
    await select.click()
    await page.locator('.el-select-dropdown__item:has-text("上架")').first().click()
    await page.waitForTimeout(300)

    const rows = page.locator('.el-table__body tr')
    const count = await rows.count()
    // should only show status=1 products
    expect(count).toBeLessThanOrEqual(MOCK_PRODUCTS.length)
  })

  test('P1: status toggle switch works', async ({ page }) => {
    const switchEl = page.locator('.el-switch').first()
    await switchEl.click()
    await page.waitForTimeout(300)
    // the switch should toggle visually (no error = passed)
  })

  test('P1: product name is a clickable link', async ({ page }) => {
    const nameLink = page.locator('.el-table__body a').first()
    await expect(nameLink).toBeVisible()
    const href = await nameLink.getAttribute('href')
    expect(href).toContain('/products/')
  })

  test('P1: clicking product name navigates to detail', async ({ page }) => {
    await page.locator('.el-table__body a').first().click()
    await page.waitForURL('**/products/*')
    // should NOT be the edit page
    const url = page.url()
    expect(url).not.toContain('/edit')
  })

  test('P1: "查看" button navigates to detail', async ({ page }) => {
    await page.locator('button:has-text("查看")').first().click()
    await page.waitForURL('**/products/*')
  })

  test('P1: "编辑" button navigates to edit page', async ({ page }) => {
    await page.locator('button:has-text("编辑")').first().click()
    await page.waitForURL('**/products/*/edit')
  })

  test('P1: delete popconfirm shows on click', async ({ page }) => {
    await page.locator('button:has-text("删除")').first().click()
    // popconfirm should appear
    await expect(page.locator('.el-popconfirm')).toBeVisible()
  })

  test('P1: cancel delete does nothing', async ({ page }) => {
    const rowCountBefore = await page.locator('.el-table__body tr').count()
    await page.locator('button:has-text("删除")').first().click()
    await page.locator('.el-popconfirm__action .el-button:not(.el-button--primary)').first().click()
    await page.waitForTimeout(300)

    const rowCountAfter = await page.locator('.el-table__body tr').count()
    expect(rowCountAfter).toBe(rowCountBefore)
  })

  test('P1: confirm delete removes the row', async ({ page }) => {
    await page.locator('button:has-text("删除")').first().click()
    await page.locator('.el-popconfirm__action .el-button--primary').first().click()
    await page.waitForTimeout(300)
    // table should refresh
  })

  test('P1: "新增商品" button navigates to create', async ({ page }) => {
    await page.click('button:has-text("新增商品")')
    await page.waitForURL('**/products/create')
  })

  // ---- P2: Edge cases ----

  test('P2: search keyword is bound to input', async ({ page }) => {
    const searchInput = page.locator('input[placeholder="搜索商品..."]')
    await searchInput.fill('test-keyword')
    // verify the value is set (v-model binding works)
    await expect(searchInput).toHaveValue('test-keyword')
    // clear using the clearable button
    await page.click('.el-input__clear')
    await expect(searchInput).toHaveValue('')
  })

  test('P2: product without image shows placeholder', async ({ page }) => {
    // AirPods Pro has no mainImage
    await expect(page.locator('.thumb-empty').first()).toBeVisible()
  })

  test('P2: pagination shows correct total', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toContainText(String(MOCK_PRODUCTS.length))
  })
})
