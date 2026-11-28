import { randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { APIResponse, Response } from '@playwright/test'
import { expect, test } from '@playwright/test'

async function result(response: APIResponse | Response) {
  expect(response.ok(), `${response.url()} returned HTTP ${response.status()}`).toBe(true)
  const body = await response.json()
  expect(body.code, body.message).toBe(200)
  return body.data
}

const repository = fileURLToPath(new URL('../../../', import.meta.url))

function orderSql(statement: string) {
  return execFileSync('docker', [
    'compose', 'exec', '-T', 'mysql', 'sh', '-c',
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -Nse "$1" ecommerce_order', 'mysql', statement,
  ], { cwd: repository, encoding: 'utf-8', timeout: 5000 }).trim()
}

test('seeded SKU goes from cart to paid order and consumes one unit of available stock', async ({ page, request }) => {
  test.setTimeout(90_000)
  const login = { username: `e2e_${randomUUID().slice(0, 12)}`, password: randomUUID().slice(0, 30) }
  const account = await result(await request.post('/api/v1/auth/register', { data: login }))
  const headers = { Authorization: `Bearer ${account.token}` }
  await result(await request.post('/api/v1/users/addresses', {
    headers,
    data: {
      receiverName: '冒烟测试', receiverPhone: '13800000001', province: '北京市',
      city: '北京市', district: '朝阳区', detail: '测试地址 1 号', isDefault: 1,
    },
  }))

  const skuId = '1'
  const stockBefore = await result(await request.get(`/api/v1/inventory/${skuId}`, { headers }))
  expect(stockBefore.availableStock).toBeGreaterThan(0)

  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(login.username)
  await page.getByPlaceholder('密码').fill(login.password)
  const signedIn = page.waitForResponse(res =>
    new URL(res.url()).pathname === '/api/v1/auth/login' && res.request().method() === 'POST')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await result(await signedIn)
  await expect(page).toHaveURL(/\/$/)

  await page.goto('/products/1')
  await expect(page.getByRole('heading', { name: '海尔双开门冰箱 BCD-500' })).toBeVisible()
  await page.getByRole('button', { name: /BCD-500 银灰色/ }).click()
  const added = page.waitForResponse(res =>
    new URL(res.url()).pathname === '/api/v1/cart/items' && res.request().method() === 'POST')
  await page.getByRole('button', { name: '加入购物车', exact: true }).click()
  await result(await added)

  await page.goto('/cart')
  await expect(page.getByText('海尔双开门冰箱 BCD-500 - BCD-500 银灰色')).toBeVisible()
  await page.getByRole('button', { name: '去结算' }).click()
  await expect(page).toHaveURL(/\/checkout$/)
  await expect(page.getByText(/测试地址 1 号/)).toBeVisible()
  const submitted = page.waitForResponse(res =>
    new URL(res.url()).pathname === '/api/v1/orders' && res.request().method() === 'POST')
  await page.getByRole('button', { name: '提交订单' }).click()
  const { orderNo } = await result(await submitted)
  expect(orderNo).toMatch(/^[A-Z0-9]+$/)
  await expect(page).toHaveURL(new RegExp(`/payment/${orderNo}$`))

  await expect.poll(async () => {
    const stock = await result(await request.get(`/api/v1/inventory/${skuId}`, { headers }))
    return stock.availableStock
  }, { timeout: 30_000 }).toBe(stockBefore.availableStock - 1)

  await page.getByRole('button', { name: '确认支付' }).click()
  await expect(page.getByText('支付成功！')).toBeVisible()
  const getOrder = async () => result(await request.get(`/api/v1/orders/no/${orderNo}`, { headers }))
  await expect.poll(async () => (await getOrder()).status, { timeout: 30_000 }).toBe(1)
  const order = await getOrder()
  expect(order.orderNo).toBe(orderNo)
  expect(order.items).toMatchObject([{ skuId, quantity: 1 }])

  const stockAfter = await result(await request.get(`/api/v1/inventory/${skuId}`, { headers }))
  expect(stockAfter.availableStock).toBe(stockBefore.availableStock - 1)
  await page.getByRole('link', { name: /查看订单/ }).click()
  await expect(page.getByText(orderNo, { exact: false })).toBeVisible()

  if (process.env.PLAYWRIGHT_VERIFY_OUTBOX === '1') {
    await test.step('reclaim an expired send and consume the replay without deducting stock again', async () => {
      const row = `aggregate_id = '${orderNo}' AND topic = 'order-created'`
      const status = () => orderSql(`SELECT status FROM message_outbox WHERE ${row}`)
      await expect.poll(status, { timeout: 30_000 }).toBe('2')
      expect(orderSql(`UPDATE message_outbox SET status = 1, next_retry_at = '2000-01-01 00:00:00'
                       WHERE ${row} AND status = 2; SELECT ROW_COUNT()`)).toBe('1')
      await expect.poll(status, { timeout: 30_000 }).toBe('2')

      const log = resolve(repository, process.env.DEMO_LOG_DIR || '.build-local/demo', 'ecommerce-inventory.log')
      await expect.poll(() => readFileSync(log, 'utf-8'), { timeout: 30_000 })
        .toContain(`Skip duplicated inventory event: topic=order-created orderNo=${orderNo}`)
      const stock = await result(await request.get(`/api/v1/inventory/${skuId}`, { headers }))
      expect(stock.availableStock).toBe(stockBefore.availableStock - 1)
      expect((await getOrder()).status).toBe(1)
    })
  }
})
