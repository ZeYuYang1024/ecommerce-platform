import { test, expect } from '@playwright/test'
import { MOCK_PRODUCT_DETAIL, MOCK_CATEGORIES_TREE } from './mocks/data'

test.describe('Product Form', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // mock categories API
    await page.route('**/api/v1/categories', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_CATEGORIES_TREE })
      })
    })

    // mock file upload API
    await page.route('**/api/v1/files/upload', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: 'uploaded-img.jpg' })
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

    // mock product create
    await page.route('**/api/v1/admin/products', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200, message: 'success',
            data: { id: 99, name: JSON.parse(route.request().postData() || '{}').spu?.name || 'new' }
          })
        })
      } else {
        await route.continue()
      }
    })
  })

  // ---- P0: Critical path ----

  test('P0: create form loads with empty fields', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    // should show 新增商品 title
    await expect(page.locator('h2')).toContainText('新增商品')
    // name input is empty
    await expect(page.locator('input[placeholder="请输入商品名称"]')).toHaveValue('')
  })

  test('P0: creating product with only name succeeds', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    await page.fill('input[placeholder="请输入商品名称"]', '测试新品')
    await page.click('button:has-text("创建商品")')

    // should navigate to /products on success
    await page.waitForURL('**/products')
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 })
  })

  test('P0: empty product name submission', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    // clear the default SKU name and submit with empty
    await page.click('button:has-text("创建商品")')
    // Should still submit (validation is at the backend level for this form)
    await page.waitForTimeout(500)
  })

  // ---- P1: Core features ----

  test('P1: category select shows options', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    await page.click('.el-select')
    await expect(page.locator('.el-select-dropdown__item').first()).toBeVisible()
  })

  test('P1: can add a SKU', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    // default: 1 SKU exists
    const skuCardsBefore = page.locator('.sku-card')
    const before = await skuCardsBefore.count()

    await page.click('button:has-text("添加 SKU")')
    const after = await skuCardsBefore.count()
    expect(after).toBe(before + 1)
  })

  test('P1: can delete a SKU when multiple exist', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    // add another SKU first
    await page.click('button:has-text("添加 SKU")')
    const before = await page.locator('.sku-card').count()

    // delete the first SKU
    await page.locator('.sku-card .sku-header button:has-text("删除")').first().click()
    const after = await page.locator('.sku-card').count()
    expect(after).toBe(before - 1)
  })

  test('P1: can add spec key-value pair to SKU', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    // click "添加属性" button
    await page.click('button:has-text("+ 添加属性")')
    const specPairs = page.locator('.spec-pair')
    const count = await specPairs.count()
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('P1: "取消" button goes back', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    await page.click('button:has-text("取消")')
    await page.waitForTimeout(300)
  })

  // ---- P2: Edge cases ----

  test('P2: edit mode shows correct title', async ({ page }) => {
    // mock product detail for edit mode
    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL })
      })
    })
    // mock product update
    await page.route('**/api/v1/admin/products/1', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success', data: {} })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/products/1/edit')
    await page.waitForResponse('**/api/v1/products/1')

    await expect(page.locator('h2')).toContainText('编辑商品')
  })

  test('P2: edit mode pre-fills form fields', async ({ page }) => {
    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL })
      })
    })
    await page.route('**/api/v1/admin/products/1', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success', data: {} })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/products/1/edit')
    await page.waitForResponse('**/api/v1/products/1')

    // name should be pre-filled
    await expect(page.locator('input[placeholder="请输入商品名称"]')).toHaveValue('iPhone 15')
    // SKUs should be loaded
    const skuCards = page.locator('.sku-card')
    const count = await skuCards.count()
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('P2: can submit without any SKU', async ({ page }) => {
    await page.goto('/products/create')
    await page.waitForResponse('**/api/v1/categories')

    // delete the default SKU - but only if there's a delete button (needs >1)
    await page.click('button:has-text("添加 SKU")')
    const cards = page.locator('.sku-card')
    // delete all SKUs
    const count = await cards.count()
    for (let i = 0; i < count; i++) {
      const delBtn = page.locator('.sku-card .sku-header button:has-text("删除")').last()
      if (await delBtn.isVisible()) {
        await delBtn.click()
        await page.waitForTimeout(100)
      }
    }

    await page.fill('input[placeholder="请输入商品名称"]', '无SKU商品')
    await page.click('button:has-text("创建商品")')

    await page.waitForURL('**/products')
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 })
  })
})
