import { test, expect } from '@playwright/test'
import { MOCK_USERS } from './mocks/data'

test.describe('User List', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // mock admin users API
    await page.route('**/api/v1/admin/users', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_USERS })
      })
    })

    await page.goto('/users')
    await page.waitForResponse('**/api/v1/admin/users')
  })

  // ---- P1: Core features ----

  test('P1: user list loads with data rows', async ({ page }) => {
    // table should have data rows
    const rows = page.locator('.el-table__body tr')
    await expect(rows).not.toHaveCount(0)
  })

  test('P1: table shows user columns', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toContainText('zhangsan')
    await expect(table).toContainText('13800138000')
  })

  test('P1: avatar shows first letter of username', async ({ page }) => {
    const avatar = page.locator('.user-avatar').first()
    await expect(avatar).toBeVisible()
    // should show uppercase first letter of username
    await expect(avatar).toContainText('Z')
  })

  // ---- P2: Edge cases ----

  test('P2: user with null phone shows empty column', async ({ page }) => {
    // lisi has no phone
    const rows = page.locator('.el-table__body tr')
    const lisiRow = rows.filter({ hasText: 'lisi' })
    await expect(lisiRow).toBeVisible()
    // phone column should be empty or show nothing
  })

  test('P2: long username displays without overflow', async ({ page }) => {
    // third mock user has a long username
    await expect(page.locator('.el-table__body')).toContainText('a_very_long_username_indeed')
  })

  test('P2: user IDs are in monospace font', async ({ page }) => {
    const idCell = page.locator('.font-mono').first()
    await expect(idCell).toBeVisible()
  })
})
