import { test, expect } from '@playwright/test'

const MOCK_MERCHANTS = [
  { id: 1, name: '测试店铺A', contactName: '张三', contactPhone: '13800138000', status: 0, statusText: '待审核', createdAt: '2026-05-01T10:00:00' },
  { id: 2, name: '测试店铺B', contactName: '李四', contactPhone: '13900139000', status: 1, statusText: '已通过', createdAt: '2026-05-02T10:00:00' },
  { id: 3, name: '测试店铺C', contactName: '王五', contactPhone: '13700137000', status: 2, statusText: '已驳回', reason: '资质不全', createdAt: '2026-04-15T08:30:00' },
]

test.describe('Merchant List', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    await page.route('**/api/v1/admin/merchants*', async (route, req) => {
      const url = new URL(req.url())
      const status = url.searchParams.get('status')
      let filtered = [...MOCK_MERCHANTS]
      if (status !== null && status !== '') {
        filtered = filtered.filter(m => m.status === Number(status))
      }
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          data: { records: filtered, total: filtered.length, current: 1, size: 10 }
        })
      })
    })

    await page.route('**/api/v1/admin/merchants/*/audit', async (route) => {
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'success', data: {} })
      })
    })

    await page.route('**/api/v1/admin/merchants/*', async (route) => {
      if (route.request().method() === 'GET') {
        const id = route.request().url().split('/').pop()
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: MOCK_MERCHANTS.find(m => m.id === Number(id)) || MOCK_MERCHANTS[0] })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/merchants')
    await page.waitForTimeout(500)
  })

  // P0 核心场景 / P0 core scenarios
  test('P0: merchant list loads with data', async ({ page }) => {
    await expect(page.locator('.el-table__body tr')).not.toHaveCount(0)
  })

  test('P0: status filter works', async ({ page }) => {
    await page.click('.el-select')
    await page.locator('.el-select-dropdown__item:has-text("待审核")').click()
    await page.waitForTimeout(300)
    const rows = page.locator('.el-table__body tr')
    await expect(rows).toHaveCount(1)
    await expect(rows.first()).toContainText('测试店铺A')
  })

  test('P0: audit dialog opens', async ({ page }) => {
    await page.locator('button:has-text("审核")').first().click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog')).toContainText('测试店铺A')
  })

  test('P0: approve merchant', async ({ page }) => {
    await page.locator('button:has-text("审核")').first().click()
    await page.locator('.el-dialog button:has-text("通过")').click()
    await page.waitForTimeout(300)
    // 弹窗应该关闭 / dialog should close
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 3000 })
  })

  test('P0: reject merchant', async ({ page }) => {
    await page.locator('button:has-text("审核")').first().click()
    await page.locator('.el-dialog button:has-text("驳回")').click()
    await page.waitForTimeout(300)
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 3000 })
  })

  // P1 主要交互 / P1 main interactions
  test('P1: "查看" navigates to detail', async ({ page }) => {
    await page.locator('button:has-text("查看")').first().click()
    await page.waitForURL('**/merchants/*')
  })

  test('P1: ban merchant shows confirm', async ({ page }) => {
    // 先切到已通过标签 / switch to approved tab first
    await page.click('.el-select')
    await page.locator('.el-select-dropdown__item:has-text("已通过")').click()
    await page.waitForTimeout(300)
    await page.locator('button:has-text("关停")').first().click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog')).toContainText('关停')
  })

  test('P1: cancel audit dialog', async ({ page }) => {
    await page.locator('button:has-text("审核")').first().click()
    await page.locator('.el-dialog button:has-text("取消")').click()
    await page.waitForTimeout(300)
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 3000 })
  })

  // P2 边界场景 / P2 edge scenarios
  test('P2: status tags show correct colors', async ({ page }) => {
    await expect(page.locator('.el-tag--warning').first()).toBeVisible()
  })

  test('P2: approved merchant shows no audit button', async ({ page }) => {
    await page.click('.el-select')
    await page.locator('.el-select-dropdown__item:has-text("已通过")').click()
    await page.waitForTimeout(300)
    await expect(page.locator('button:has-text("审核")')).toHaveCount(0)
  })
})
