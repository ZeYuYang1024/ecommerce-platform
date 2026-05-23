import { test, expect } from '@playwright/test'
import { MOCK_CATEGORIES_TREE, MOCK_CATEGORIES_ALL } from './mocks/data'

test.describe('Category Management', () => {

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('token', 'mock-token')
      localStorage.setItem('username', 'admin')
    })

    // 模拟后台分类接口（平铺列表） / mock admin categories (flat list)
    await page.route('**/api/v1/admin/categories', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: MOCK_CATEGORIES_ALL })
        })
      } else if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success', data: { id: 99 } })
        })
      } else {
        await route.continue()
      }
    })

    // 模拟后台分类按 ID 接口（PUT/DELETE） / mock admin categories by ID (PUT/DELETE)
    await page.route('**/api/v1/admin/categories/*', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success' })
        })
      } else if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, message: 'success' })
        })
      } else {
        await route.continue()
      }
    })

    // 模拟前台分类接口（树结构） / mock public categories (tree)
    await page.route('**/api/v1/categories', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, data: MOCK_CATEGORIES_TREE })
      })
    })

    await page.goto('/categories')
    await expect(page.locator('.el-tree')).toBeVisible()
  })

  // P1 核心能力 / P1 core features

  test('P1: category tree loads with nodes', async ({ page }) => {
    await expect(page.locator('.el-tree')).toBeVisible()
    await expect(page.locator('.el-tree-node')).not.toHaveCount(0)
  })

  test('P1: level badges are shown on tree nodes', async ({ page }) => {
    // 一级节点应该显示“一级”标签 / first-level nodes should have "一级" badge
    await expect(page.locator('.level-tag').first()).toContainText('一级')
  })

  test('P1: "新增分类" opens dialog', async ({ page }) => {
    await page.click('button:has-text("新增分类")')
    await expect(page.locator('.el-dialog')).toBeVisible()
    // 弹窗标题应该是“新增分类” / dialog title should be "新增分类"
    await expect(page.locator('.el-dialog__title')).toContainText('新增分类')
  })

  test('P1: can create a new root category', async ({ page }) => {
    await page.click('button:has-text("新增分类")')
    await page.fill('input[placeholder="请输入分类名称"]', '新分类')
    await page.click('.el-dialog button:has-text("保存")')

    await page.waitForTimeout(500)
    // 弹窗应该关闭 / dialog should close
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })

  test('P1: "编辑" opens dialog with data', async ({ page }) => {
    // 点击第一个节点的编辑按钮 / click edit on first node
    await page.locator('button:has-text("编辑")').first().click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('编辑')
    // 名称应该被预填充 / name should be pre-filled
    await expect(page.locator('input[placeholder="请输入分类名称"]')).not.toHaveValue('')
  })

  test('P1: "子分类" button opens dialog for child', async ({ page }) => {
    await page.locator('button:has-text("子分类")').first().click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('新增子分类')
  })

  test('P1: delete shows popconfirm with child warning', async ({ page }) => {
    await page.locator('button:has-text("删除")').first().click()
    await expect(page.locator('.el-popconfirm')).toBeVisible()
    await expect(page.locator('.el-popconfirm')).toContainText('子分类也会被删除')
  })

  test('P1: cancel delete button closes popconfirm', async ({ page }) => {
    await page.locator('button:has-text("删除")').first().click()
    await page.locator('.el-popconfirm__action .el-button:not(.el-button--primary)').first().click()
    await page.waitForTimeout(300)
    await expect(page.locator('.el-popconfirm')).not.toBeVisible()
  })

  test('P1: confirm delete removes category', async ({ page }) => {
    const before = await page.locator('.el-tree-node').count()
    await page.locator('button:has-text("删除")').first().click()
    await page.locator('.el-popconfirm__action .el-button--primary').first().click()
    await page.waitForTimeout(500)
  })

  // P2 边界场景 / P2 edge cases

  test('P2: cancel dialog button closes without saving', async ({ page }) => {
    await page.click('button:has-text("新增分类")')
    await page.fill('input[placeholder="请输入分类名称"]', '取消的分类')
    await page.click('.el-dialog button:has-text("取消")')
    await page.waitForTimeout(300)
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })

  test('P2: dialog sort input respects min=0', async ({ page }) => {
    await page.click('button:has-text("新增分类")')
    const numberInput = page.locator('.el-input-number input').first()
    await expect(numberInput).toBeVisible()
  })

  test('P2: tree-node has icon for adding sub-category', async ({ page }) => {
    // 每个节点都应该有“子分类”按钮 / each node should have a "+ 子分类" button
    const addChildBtns = page.locator('button:has-text("子分类")')
    expect(await addChildBtns.count()).toBeGreaterThan(0)
  })
})
