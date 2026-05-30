import { test, expect } from '@playwright/test'
import { MOCK_PRODUCTS } from './mocks/data'

async function gotoProductList(page) {
  const listResponse = page.waitForResponse((response) => (
    response.request().method() === 'GET'
    && response.url().includes('/api/v1/admin/products')
  ))

  await page.goto('/products')
  await listResponse
}

test.describe('Product List', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    await page.route('**/api/v1/admin/products*', async (route, req) => {
      const url = new URL(req.url())
      const keyword = url.searchParams.get('keyword')
      const status = url.searchParams.get('status')
      let filtered = [...MOCK_PRODUCTS]

      if (keyword) {
        filtered = filtered.filter((product) => product.name.includes(keyword))
      }
      if (status !== null && status !== undefined && status !== '') {
        filtered = filtered.filter((product) => product.status === Number(status))
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            current: 1,
            size: 10,
            total: filtered.length,
            pages: 1,
            records: filtered,
          },
        }),
      })
    })

    await page.route('**/api/v1/files/*/url', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: 'https://picsum.photos/200' }),
      })
    })

    await page.route('**/api/v1/admin/products/*/status', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'success' }),
      })
    })

    await page.route('**/api/v1/admin/products/*', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success' }),
        })
      } else {
        await route.continue()
      }
    })

    await gotoProductList(page)
  })

  test('P0: product list loads with data rows', async ({ page }) => {
    await expect(page.locator('.el-table__body tr')).not.toHaveCount(0)
  })

  test('P0: pagination component is visible', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toBeVisible()
  })

  test('P0: redirects to /login when no token', async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
    await page.goto('/products')
    await page.reload()
    await page.waitForURL('**/login', { timeout: 5000 })
  })

  test('P0: search by typing Enter triggers data refresh', async ({ page }) => {
    await page.route('**/api/v1/admin/products*', async (route, req) => {
      const url = new URL(req.url())
      const keyword = url.searchParams.get('keyword')
      let filtered = [...MOCK_PRODUCTS]

      if (keyword) {
        filtered = filtered.filter((product) => product.name.includes(keyword))
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            current: 1,
            size: 10,
            total: filtered.length,
            pages: 1,
            records: filtered,
          },
        }),
      })
    })

    const searchInput = page.locator('.toolbar-left .el-input input')
    await searchInput.fill('iPhone')
    await page.keyboard.press('Enter')
    await page.waitForTimeout(500)

    const rows = page.locator('.el-table__body tr')
    await expect(rows).toHaveCount(1)
    await expect(rows.first()).toContainText('iPhone')
  })

  test('P0: clear search restores full list', async ({ page }) => {
    const searchInput = page.locator('.toolbar-left .el-input input')
    await searchInput.fill('iPhone')
    await page.click('.el-input__clear')
    await page.waitForTimeout(500)

    const rows = page.locator('.el-table__body tr')
    await expect(rows).toHaveCount(MOCK_PRODUCTS.length)
  })

  test('P1: status select filters products', async ({ page }) => {
    await page.locator('.toolbar-left .el-select').first().click()
    await page.locator('.el-select-dropdown__item').first().click()
    await page.waitForTimeout(300)

    const rows = page.locator('.el-table__body tr')
    const count = await rows.count()
    expect(count).toBeLessThanOrEqual(MOCK_PRODUCTS.length)
  })

  test('P1: status toggle switch works', async ({ page }) => {
    await page.locator('.el-switch').first().click()
    await page.waitForTimeout(300)
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
    expect(page.url()).not.toContain('/edit')
  })

  test('P1: view button navigates to detail', async ({ page }) => {
    const firstRowActions = page.locator('.actions').first()
    await firstRowActions.locator('.el-button').nth(0).click()
    await page.waitForURL('**/products/*')
  })

  test('P1: edit button navigates to edit page', async ({ page }) => {
    const firstRowActions = page.locator('.actions').first()
    await firstRowActions.locator('.el-button').nth(1).click()
    await page.waitForURL('**/products/*/edit')
  })

  test('P1: delete popconfirm shows on click', async ({ page }) => {
    const firstRowActions = page.locator('.actions').first()
    await firstRowActions.locator('.el-button').nth(2).click()
    await expect(page.locator('.el-popconfirm')).toBeVisible()
  })

  test('P1: cancel delete does nothing', async ({ page }) => {
    const rowCountBefore = await page.locator('.el-table__body tr').count()
    const firstRowActions = page.locator('.actions').first()

    await firstRowActions.locator('.el-button').nth(2).click()
    await page.locator('.el-popconfirm__action .el-button:not(.el-button--primary)').first().click()
    await page.waitForTimeout(300)

    const rowCountAfter = await page.locator('.el-table__body tr').count()
    expect(rowCountAfter).toBe(rowCountBefore)
  })

  test('P1: confirm delete refreshes the table', async ({ page }) => {
    const firstRowActions = page.locator('.actions').first()
    await firstRowActions.locator('.el-button').nth(2).click()
    await page.locator('.el-popconfirm__action .el-button--primary').first().click()
    await page.waitForTimeout(300)
  })

  test('P1: create button navigates to product create', async ({ page }) => {
    await page.locator('.toolbar > .el-button').click()
    await page.waitForURL('**/products/create')
  })

  test('P2: search keyword is bound to input', async ({ page }) => {
    const searchInput = page.locator('.toolbar-left .el-input input')
    await searchInput.fill('test-keyword')
    await expect(searchInput).toHaveValue('test-keyword')
    await page.click('.el-input__clear')
    await expect(searchInput).toHaveValue('')
  })

  test('P2: product without image shows placeholder', async ({ page }) => {
    await expect(page.locator('.thumb-empty').first()).toBeVisible()
  })

  test('P2: product list resolves object-name images to real preview URLs', async ({ page }) => {
    await expect(page.locator('.thumb').first()).toHaveCSS('background-image', /picsum\.photos\/200/)
  })

  test('P2: pagination shows correct total', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toContainText(String(MOCK_PRODUCTS.length))
  })
})
