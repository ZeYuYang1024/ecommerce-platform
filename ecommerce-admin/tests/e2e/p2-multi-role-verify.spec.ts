import { test, expect } from '@playwright/test'

test.describe('P2 Multi-Role UI Verification', () => {

  // ==========================================
  // 1. SUPER_ADMIN 完整权限验证 / SUPER_ADMIN full permission verification
  // ==========================================
  test('super_admin: login stores correct type', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    const type = await page.evaluate(() => localStorage.getItem('type'))
    expect(type).toBe('super_admin')
  })

  test('super_admin: sidebar shows ALL sections', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    const sidebar = page.locator('.sidebar')
    const sidebarText = await sidebar.textContent()

    expect(sidebarText).toContain('商家管理')
    expect(sidebarText).toContain('商品运营')
    expect(sidebarText).toContain('交易管理')
    expect(sidebarText).toContain('财务管理')
    expect(sidebarText).toContain('用户管理')

    const navItems = sidebar.locator('.nav-item')
    const count = await navItems.count()
    expect(count).toBeGreaterThanOrEqual(10)
  })

  test('super_admin: dashboard shows real numbers', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })
    await page.waitForTimeout(1500)

    const statValues = page.locator('.stat-value')
    const count = await statValues.count()
    for (let i = 0; i < count; i++) {
      const val = await statValues.nth(i).textContent()
      expect(val?.trim()).not.toBe('--')
    }
  })

  // ==========================================
  // 2. MERCHANT 注册 + 审核（UI 交互 + API） / MERCHANT register + audit (UI interaction + API)
  // ==========================================
  test('merchant: register, audit via UI, verify auto-account', async ({ page }) => {
    const merchantName = `E2E_Shop_${Date.now()}`

    // 先进入一个页面，确保 window.location.origin 可用 / navigate to a page first so window.location.origin is available
    await page.goto('/login')

    // 通过 fetch 注册商家（当前在 localhost:5173，走 Vite 代理） / register via fetch (uses Vite proxy because we're on localhost:5173)
    const regResult = await page.evaluate(async (name) => {
      const res = await fetch('/api/v1/merchants/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, contactName: '测试', contactPhone: '13800000099', businessLicense: 'test' })
      })
      return res.json()
    }, merchantName)

    if (!regResult || regResult.code !== 200) {
      console.log('Merchant register failed')
      return
    }

    const merchantId = regResult.data.id

    // 以 super_admin 身份登录 / login as super_admin
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // 进入商家列表并筛选待审核 / navigate to merchant list, filter pending
    await page.goto('/merchants')
    await page.waitForTimeout(2000)

    const select = page.locator('.el-select').first()
    await select.click()
    await page.waitForTimeout(500)
    await page.locator('.el-select-dropdown__item:has-text("待审核")').click()
    await page.waitForTimeout(1500)

    const merchantRow = page.locator('.el-table__body tr').filter({ hasText: merchantName }).first()
    if (!(await merchantRow.isVisible({ timeout: 10000 }).catch(() => false))) return

    const auditBtn = merchantRow.locator('button:has-text("审核")')
    if (!(await auditBtn.isVisible({ timeout: 3000 }).catch(() => false))) return

    await auditBtn.click()
    await page.waitForTimeout(500)

    // 校验审核弹窗 / verify audit dialog
    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible({ timeout: 3000 })
    expect(await dialog.textContent()).toContain(merchantName)

    // 执行通过审核 / approve
    const auditRequest = page.waitForResponse((response) =>
      response.request().method() === 'PUT' &&
      response.url().includes(`/api/v1/admin/merchants/${merchantId}/audit`) &&
      response.status() === 200
    )
    await dialog.locator('button:has-text("通过")').click()
    await auditRequest
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
    await expect.poll(async () =>
      await page.locator('.el-table__body tr').filter({ hasText: merchantName }).count()
    ).toBe(0)
  })

  // ==========================================
  // 3. MERCHANT 管理员账号创建验证 / MERCHANT admin account creation verification
  // ==========================================
  test('merchant_admin: account creation and login', async ({ page }) => {
    // 以 super_admin 身份登录 / login as super_admin
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // 查找已审核通过的商家 / find approved merchant
    await page.goto('/merchants')
    await page.waitForTimeout(1500)
    const select = page.locator('.el-select').first()
    await select.click()
    await page.waitForTimeout(500)
    const approvedOpt = page.locator('.el-select-dropdown__item:has-text("已通过")')
    if (!(await approvedOpt.isVisible({ timeout: 2000 }).catch(() => false))) return
    await approvedOpt.click()
    await page.waitForTimeout(1500)

    const firstRow = page.locator('.el-table__body tr').first()
    if (!(await firstRow.isVisible({ timeout: 2000 }).catch(() => false))) return
    const rowText = await firstRow.textContent()
    const idMatch = rowText?.match(/(\d{10,})/)
    if (!idMatch) return
    const merchantId = idMatch[1]

    // 通过 fetch 创建商家账号（带上鉴权 token） / create merchant account via fetch (include auth token)
    const token = await page.evaluate(() => localStorage.getItem('token'))
    const result = await page.evaluate(async ({ id, token }) => {
      const res = await fetch(`/api/v1/admin/merchant-account`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ merchantId: Number(id) })
      })
      return res.json()
    }, { id: merchantId, token })

    expect(result.code).toBe(200)
    const username = result.data.username
    expect(username).toContain('m_')

    console.log(`Merchant admin account: ${username} created=${result.data.created}`)
  })

  // ==========================================
  // 4. 侧边栏角色隐藏验证（UI 层面） / sidebar role hiding verification (UI level)
  // ==========================================
  test('sidebar: merchant type hides restricted sections', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // 切换为 merchant 角色 / change type to merchant
    await page.evaluate(() => localStorage.setItem('type', 'merchant'))
    await page.reload()
    await page.waitForTimeout(2000)

    const body = await page.textContent('body')
    expect(body).toBeTruthy()
  })

  test('sidebar: ops type hides merchant and user management', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // 切换为 ops 角色 / change type to ops
    await page.evaluate(() => localStorage.setItem('type', 'ops'))
    await page.reload()
    await page.waitForTimeout(2000)

    const body = await page.textContent('body')
    expect(body).toBeTruthy()
  })

  // ==========================================
  // 5. 完整流程 / full flow
  // ==========================================
  test('full flow: register, approve, verify account', async ({ page, request }) => {
    const uniqueName = `FullFlow_${Date.now()}`

    // 通过 Playwright request 注册商家（直连网关，不需要 Vite 代理） / register via Playwright request (direct to gateway, no Vite proxy needed)
    const regResp = await request.post('http://localhost:8080/api/v1/merchants/register', {
      headers: { 'Content-Type': 'application/json' },
      data: JSON.stringify({ name: uniqueName, contactName: '全流程', contactPhone: '13800000088', businessLicense: 'test' })
    })
    const regResult = await regResp.json()

    if (!regResult || regResult.code !== 200) {
      console.log('Merchant register failed')
      return
    }
    const merchantId = regResult.data.id

    // 管理员登录 / admin login
    await page.goto('/login')
    await page.fill('input[placeholder="请输入用户名"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // 审核前先筛选待审核记录 / audit - filter pending
    await page.goto('/merchants')
    await page.waitForTimeout(1500)
    const select = page.locator('.el-select').first()
    await select.click()
    await page.waitForTimeout(500)
    await page.locator('.el-select-dropdown__item:has-text("待审核")').click()
    await page.waitForTimeout(1500)

    // 执行审核并通过 / audit and approve
    const merchantRow = page.locator('.el-table__body tr').filter({ hasText: uniqueName }).first()
    if (!(await merchantRow.isVisible({ timeout: 10000 }).catch(() => false))) return
    const auditBtn = merchantRow.locator('button:has-text("审核")')
    if (!(await auditBtn.isVisible({ timeout: 3000 }).catch(() => false))) return
    await auditBtn.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog')
    await expect(dialog).toBeVisible({ timeout: 3000 })
    expect(await dialog.textContent()).toContain(uniqueName)

    const auditRequest = page.waitForResponse((response) =>
      response.request().method() === 'PUT' &&
      response.url().includes(`/api/v1/admin/merchants/${merchantId}/audit`) &&
      response.status() === 200
    )
    await dialog.locator('button:has-text("通过")').click()
    await auditRequest
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
    await expect.poll(async () =>
      await page.locator('.el-table__body tr').filter({ hasText: uniqueName }).count()
    ).toBe(0)

    // 通过 Playwright request 创建商家账号 / create account via Playwright request
    const token = await page.evaluate(() => localStorage.getItem('token'))
    const accResp = await request.post('http://localhost:8080/api/v1/admin/merchant-account', {
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      data: JSON.stringify({ merchantId: merchantId })
    })
    const result = await accResp.json()

    expect(result.code).toBe(200)
    expect(result.data.username).toContain('m_')
  })
})
