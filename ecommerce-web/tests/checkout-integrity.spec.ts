import { expect, test } from '@playwright/test'

const cartRow = (skuId: number, name: string, available = true) => ({
  skuId, spuId: skuId, name, quantity: 1, checked: true, available, price: 20
})
const addresses = Array.from({ length: 20 }, (_, index) => ({
  id: index + 1, receiverName: `收货人 ${index + 1}`, receiverPhone: '13800000000',
  province: '北京市', city: '北京市', district: '朝阳区', detail: `测试地址 ${index + 1}`,
  isDefault: Number(index === 0)
}))

test('切换账号后丢弃旧购物车响应，结算使用当前账号', async ({ page, baseURL }) => {
  await page.context().addCookies([
    { name: 'token', value: 'buyer-a-token', url: baseURL! },
    { name: 'username', value: 'buyer-a', url: baseURL! },
  ])
  let seenA = false
  let releaseOldCart!: () => void
  const oldCart = new Promise<void>(resolve => { releaseOldCart = resolve })
  let submitted: { token?: string, skuIds: number[] } | undefined

  await page.route('**/api/v1/**', async route => {
    const { pathname } = new URL(route.request().url())
    const token = route.request().headers().authorization
    let data: unknown = []
    if (pathname.endsWith('/auth/login')) {
      data = { username: 'buyer-b', token: 'buyer-b-token' }
    } else if (pathname.endsWith('/cart')) {
      if (token === 'Bearer buyer-a-token' && !seenA) {
        seenA = true
        await oldCart
      }
      data = token === 'Bearer buyer-a-token' ? [cartRow(41, 'A 的商品')] : [cartRow(42, 'B 的商品')]
    } else if (pathname.endsWith('/addresses')) {
      data = addresses
    } else if (pathname.endsWith('/orders') && route.request().method() === 'POST') {
      submitted = { token, skuIds: route.request().postDataJSON().items.map((item: { skuId: number }) => item.skuId) }
      data = { status: 0, orderNo: 'TEST-42' }
    }
    await route.fulfill({ json: { code: 200, data } })
  })

  await page.goto('/cart')
  await expect(page.getByText('A 的商品')).toBeVisible()
  await page.getByRole('button', { name: '退出' }).click()
  await page.getByRole('link', { name: '登录', exact: true }).click()
  await page.getByPlaceholder('用户名').fill('buyer-b')
  await page.getByPlaceholder('密码').fill('test-password')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.getByRole('link', { name: 'buyer-b' })).toBeVisible()
  await page.locator('a[href="/cart"]').first().click()
  await expect(page.getByText('B 的商品')).toBeVisible()
  releaseOldCart()
  await page.getByRole('button', { name: '去结算' }).click()
  await expect(page.getByText('B 的商品')).toBeVisible()
  await expect(page.getByText('A 的商品')).toHaveCount(0)
  await page.getByRole('button', { name: '提交订单' }).click()
  await expect.poll(() => submitted).toEqual({ token: 'Bearer buyer-b-token', skuIds: [42] })
})

test('已选下架商品需确认跳过，地址保存失败有提示和管理入口', async ({ page, baseURL }) => {
  await page.context().addCookies([{ name: 'token', value: 'buyer-token', url: baseURL! }])
  await page.route('**/api/v1/**', async route => {
    const { pathname } = new URL(route.request().url())
    if (pathname.endsWith('/addresses') && route.request().method() === 'POST') {
      await route.fulfill({ json: { code: 10010003, message: '最多保存 20 个收货地址' } })
      return
    }
    const data = pathname.endsWith('/cart')
      ? [cartRow(41, '在售商品'), cartRow(42, '下架商品', false)]
      : pathname.endsWith('/addresses') ? addresses : []
    await route.fulfill({ json: { code: 200, data } })
  })

  await page.goto('/checkout')
  await expect(page.getByText('在售商品')).toBeVisible()
  await expect(page.getByText('下架商品')).toHaveCount(0)
  await expect(page.getByRole('button', { name: '提交订单' })).toBeDisabled()
  await page.getByRole('checkbox', { name: /继续结算其余商品/ }).check()
  await expect(page.getByRole('button', { name: '提交订单' })).toBeEnabled()

  await page.getByRole('button', { name: '管理地址' }).click()
  await expect(page.getByRole('button', { name: '编辑', exact: true })).toHaveCount(20)
  await expect(page.getByRole('button', { name: '删除', exact: true })).toHaveCount(20)
  await page.getByRole('button', { name: '+ 新建地址' }).click()
  await page.getByPlaceholder('收货人', { exact: true }).fill('新收货人')
  await page.getByPlaceholder('手机号').fill('13900000000')
  await page.getByPlaceholder(/详细地址/).fill('新地址')
  await page.getByRole('button', { name: '保存地址' }).click()
  await expect(page.getByRole('alert')).toHaveText('最多保存 20 个收货地址')
})
