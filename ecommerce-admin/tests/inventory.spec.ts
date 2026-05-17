import { test, expect } from '@playwright/test'
import { MOCK_STOCK } from './mocks/data'

function createInventoryRow(overrides = {}) {
  return {
    skuId: MOCK_STOCK.skuId,
    skuName: 'iPhone 15 128GB',
    spuName: 'iPhone 15',
    price: '6999.00',
    totalStock: MOCK_STOCK.totalStock,
    lockedStock: MOCK_STOCK.lockedStock,
    availableStock: MOCK_STOCK.availableStock,
    ...overrides,
  }
}

async function searchInventory(page, skuId) {
  const responsePromise = page.waitForResponse((response) => (
    response.request().method() === 'GET'
    && response.url().includes('/api/v1/admin/inventory')
    && response.url().includes(`skuId=${skuId}`)
  ))

  await page.locator('.toolbar-left .el-input input').fill(skuId)
  await page.locator('.toolbar-right .el-button').click()
  await responsePromise
}

test.describe('Inventory Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
      localStorage.setItem('type', 'super_admin')
    })

    const state = { row: createInventoryRow() }

    await page.route('**/api/v1/admin/inventory*', async (route, req) => {
      const url = new URL(req.url())

      if (req.method() === 'POST') {
        const body = JSON.parse(req.postData() || '{}')
        state.row.totalStock = body.totalStock
        state.row.availableStock = body.totalStock - state.row.lockedStock

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success' }),
        })
        return
      }

      const skuId = url.searchParams.get('skuId')
      const records = skuId === '999' ? [] : [state.row]

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: {
            records,
            total: records.length,
            current: 1,
            size: 10,
          },
        }),
      })
    })

    await page.goto('/inventory')
    await expect(page.locator('.el-table__body tr')).toHaveCount(1)
  })

  test('P1: search by SKU ID shows stock data', async ({ page }) => {
    await searchInventory(page, '101')

    const row = page.locator('.el-table__body tr').first()
    await expect(row).toContainText('101')
    await expect(row).toContainText('500')
    await expect(row).toContainText('450')
  })

  test('P1: searching non-existent SKU shows empty state', async ({ page }) => {
    await searchInventory(page, '999')

    await expect(page.getByText('暂无库存记录')).toBeVisible()
  })

  test('P1: update stock quantity works', async ({ page }) => {
    await searchInventory(page, '101')

    await page.getByRole('button', { name: '更新库存' }).click()
    const numberInput = page.locator('.el-dialog .el-input-number input')
    await numberInput.fill('600')

    const updateRequest = page.waitForRequest((request) => (
      request.method() === 'POST'
      && request.url().includes('/api/v1/admin/inventory/101')
    ))
    const updateResponse = page.waitForResponse((response) => (
      response.request().method() === 'POST'
      && response.url().includes('/api/v1/admin/inventory/101')
    ))

    await page.getByRole('button', { name: '确认更新' }).click()
    const request = await updateRequest
    await updateResponse

    expect(JSON.parse(request.postData() || '{}').totalStock).toBe(600)
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })

  test('P2: stock number input exists and is editable', async ({ page }) => {
    await searchInventory(page, '101')

    await page.getByRole('button', { name: '更新库存' }).click()
    const numberInput = page.locator('.el-dialog .el-input-number input')
    await expect(numberInput).toBeVisible()
    await numberInput.fill('0')
    await expect(numberInput).toHaveValue('0')
  })

  test('P2: clearable input clears the search field', async ({ page }) => {
    const searchInput = page.locator('.toolbar-left .el-input input')
    await searchInput.fill('101')
    await page.click('.toolbar-left .el-input__clear')
    await expect(searchInput).toHaveValue('')
  })

  test('P2: query updates when SKU ID changes', async ({ page }) => {
    await searchInventory(page, '101')
    await expect(page).toHaveURL(/skuId=101/)

    await searchInventory(page, '999')
    await expect(page).toHaveURL(/skuId=999/)
    await expect(page.getByText('暂无库存记录')).toBeVisible()
  })

  test('P2: all stock fields are displayed correctly', async ({ page }) => {
    const row = page.locator('.el-table__body tr').first()
    await expect(row).toContainText('101')
    await expect(row).toContainText('iPhone 15 128GB')
    await expect(row).toContainText('iPhone 15')
    await expect(row).toContainText('6999.00')
    await expect(row).toContainText('500')
    await expect(row).toContainText('50')
    await expect(row).toContainText('450')
  })
})
