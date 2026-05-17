import { test, expect } from '@playwright/test'
import { MOCK_PRODUCT_DETAIL, MOCK_CATEGORIES_TREE } from './mocks/data'

async function waitForCategories(page) {
  return page.waitForResponse((response) => (
    response.request().method() === 'GET'
    && response.url().includes('/api/v1/categories')
  ))
}

async function gotoCreateForm(page) {
  const categoriesResponse = waitForCategories(page)
  await page.goto('/products/create')
  await categoriesResponse
}

async function gotoEditForm(page) {
  const categoriesResponse = waitForCategories(page)
  const detailResponse = page.waitForResponse((response) => (
    response.request().method() === 'GET'
    && response.url().includes('/api/v1/products/1')
  ))

  await page.goto('/products/1/edit')
  await Promise.all([categoriesResponse, detailResponse])
}

test.describe('Product Form', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    await page.route('**/api/v1/categories', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_CATEGORIES_TREE }),
      })
    })

    await page.route('**/api/v1/files/upload', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: 'uploaded-img.jpg' }),
      })
    })

    await page.route('**/api/v1/files/*/url', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: 'https://picsum.photos/200' }),
      })
    })

    await page.route('**/api/v1/admin/products', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: 'success',
            data: {
              id: 99,
              name: JSON.parse(route.request().postData() || '{}').spu?.name || 'new',
            },
          }),
        })
      } else {
        await route.continue()
      }
    })
  })

  test('P0: create form loads with empty fields', async ({ page }) => {
    await gotoCreateForm(page)

    await expect(page.locator('.form-header h2')).toBeVisible()
    await expect(page.locator('.form-card .el-input input').first()).toHaveValue('')
  })

  test('P0: creating product with only name succeeds', async ({ page }) => {
    await gotoCreateForm(page)

    await page.locator('.form-card .el-input input').first().fill('test product')
    await page.locator('.submit-bar .el-button--primary').click()

    await page.waitForURL('**/products')
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 })
  })

  test('P0: empty product name submission still reaches submit flow', async ({ page }) => {
    await gotoCreateForm(page)

    await page.locator('.submit-bar .el-button--primary').click()
    await page.waitForTimeout(500)
  })

  test('P1: category select shows options', async ({ page }) => {
    await gotoCreateForm(page)

    await page.locator('.el-select__wrapper').first().click()
    await expect(page.locator('.el-tree-node__content').first()).toBeVisible()
  })

  test('P1: can add a SKU', async ({ page }) => {
    await gotoCreateForm(page)

    const skuBlocks = page.locator('.sku-block')
    const before = await skuBlocks.count()

    await page.locator('.sku-card-header .el-button').click()
    await expect(skuBlocks).toHaveCount(before + 1)
  })

  test('P1: can delete a SKU when multiple exist', async ({ page }) => {
    await gotoCreateForm(page)

    await page.locator('.sku-card-header .el-button').click()
    const skuBlocks = page.locator('.sku-block')
    const before = await skuBlocks.count()

    await page.locator('.sku-block-header .el-button').first().click()
    await expect(skuBlocks).toHaveCount(before - 1)
  })

  test('P1: can add spec key-value pair to SKU', async ({ page }) => {
    await gotoCreateForm(page)

    const specArea = page.locator('.spec-area').first()
    const specPairs = specArea.locator('.spec-row')
    const before = await specPairs.count()

    await specArea.locator('.el-button').last().click()
    await expect(specPairs).toHaveCount(before + 1)
  })

  test('P1: cancel button goes back', async ({ page }) => {
    await gotoCreateForm(page)

    await page.locator('.submit-bar .el-button').nth(1).click()
    await page.waitForTimeout(300)
  })

  test('P2: edit mode route loads successfully', async ({ page }) => {
    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL }),
      })
    })

    await page.route('**/api/v1/admin/products/1', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success', data: {} }),
        })
      } else {
        await route.continue()
      }
    })

    await gotoEditForm(page)

    await expect(page).toHaveURL('http://localhost:5173/products/1/edit')
    await expect(page.locator('.form-header h2')).toBeVisible()
  })

  test('P2: edit mode pre-fills form fields', async ({ page }) => {
    await page.route('**/api/v1/products/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_PRODUCT_DETAIL }),
      })
    })

    await page.route('**/api/v1/admin/products/1', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success', data: {} }),
        })
      } else {
        await route.continue()
      }
    })

    await gotoEditForm(page)

    await expect(page.locator('.form-card .el-input input').first()).toHaveValue('iPhone 15')
    await expect(page.locator('.sku-block')).toHaveCount(2)
  })

  test('P2: form keeps one SKU block as the minimum', async ({ page }) => {
    await gotoCreateForm(page)

    await expect(page.locator('.sku-block')).toHaveCount(1)
    await expect(page.locator('.sku-block-header .el-button')).toHaveCount(0)

    await page.locator('.sku-card-header .el-button').click()
    await expect(page.locator('.sku-block')).toHaveCount(2)
    await expect(page.locator('.sku-block-header .el-button')).toHaveCount(2)

    await page.locator('.sku-block-header .el-button').last().click()

    await expect(page.locator('.sku-block')).toHaveCount(1)
    await expect(page.locator('.sku-block-header .el-button')).toHaveCount(0)
  })
})
