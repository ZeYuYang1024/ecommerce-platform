#!/bin/bash
# ============================================================
# P1 API 自动化测试 — merchant / cart / order / payment
# 前置条件：全部服务 + Gateway 已启动
# 执行方式：bash scripts/p1-api-test.sh
# ============================================================

set -e

BASE="http://localhost:8080"
PASS=0
FAIL=0
SHOP_NAME="P1Shop$(date +%s)"
ADMIN_TOKEN=""
USER_TOKEN=""
USER_ID=""
MERCHANT_ID=""
ORDER_ID=""
ORDER_NO=""
PAY_ORDER_NO=""
PAY_ORDER_ID=""

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
log_fail() { echo -e "${RED}[FAIL]${NC} $1 — $2"; FAIL=$((FAIL+1)); }
log_info() { echo -e "${CYAN}[INFO]${NC} $1"; }

extract_code() { echo "$1" | sed -n 's/.*"code": *\([0-9]*\).*/\1/p' | head -1; }
extract_token() { echo "$1" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }
extract_id() { echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }
extract_order_no() { echo "$1" | sed -n 's/.*"orderNo":"\([^"]*\)".*/\1/p'; }
extract_data_int() { echo "$1" | sed -n 's/.*"data": *\([0-9]*\).*/\1/p'; }
extract_userid() { echo "$1" | sed -n 's/.*"userId": *\([0-9]*\).*/\1/p' | head -1; }

assert_code() {
  local resp="$1" expected="$2" name="$3"
  local code=$(extract_code "$resp")
  if [ "$code" = "$expected" ]; then
    log_pass "$name"
  else
    log_fail "$name" "expected code=$expected, got code=$code"
  fi
}

# ============================
# 0. Token 获取
# ============================
log_info "=== Token ==="

RESP=$(curl -s -X POST "$BASE/api/v1/auth/admin/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
assert_code "$RESP" "200" "Admin Login"
ADMIN_TOKEN=$(extract_token "$RESP")
[ -z "$ADMIN_TOKEN" ] && log_fail "Extract AdminToken" "" && exit 1

RESP=$(curl -s -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"p1apitester","password":"123456"}')
USER_TOKEN=$(extract_token "$RESP")
USER_ID=$(extract_userid "$RESP")
if [ -z "$USER_TOKEN" ]; then
  RESP=$(curl -s -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"p1apitester","password":"123456"}')
  USER_TOKEN=$(extract_token "$RESP")
  USER_ID=$(extract_userid "$RESP")
fi
log_info "User Token: ${USER_TOKEN:0:20}... userId=$USER_ID"

# ============================
# 1. Merchant
# ============================
log_info "=== 1. Merchant ==="

RESP=$(curl -s -X POST "$BASE/api/v1/merchants/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$SHOP_NAME\",\"contactName\":\"ZhangSan\",\"contactPhone\":\"13800138000\",\"businessLicense\":\"https://example.com/lic.jpg\"}")
assert_code "$RESP" "200" "Merchant Register"
MERCHANT_ID=$(extract_id "$RESP")

RESP=$(curl -s -X POST "$BASE/api/v1/merchants/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$SHOP_NAME\",\"contactName\":\"LiSi\",\"contactPhone\":\"13900139000\",\"businessLicense\":\"url\"}")
assert_code "$RESP" "60010002" "Duplicate Shop Name"

RESP=$(curl -s "$BASE/api/v1/admin/merchants?status=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code "$RESP" "200" "List Pending Merchants"

RESP=$(curl -s "$BASE/api/v1/admin/merchants/$MERCHANT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code "$RESP" "200" "Merchant Detail"

RESP=$(curl -s -X PUT "$BASE/api/v1/admin/merchants/$MERCHANT_ID/audit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "X-User-Id: 1" \
  -d '{"action":1,"comment":"Approved"}')
assert_code "$RESP" "200" "Merchant Approve"

RESP=$(curl -s -X PUT "$BASE/api/v1/admin/merchants/$MERCHANT_ID/audit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "X-User-Id: 1" \
  -d '{"action":1}')
assert_code "$RESP" "60010004" "Dup Audit Blocked"

# ============================
# 2. Cart
# ============================
log_info "=== 2. Cart ==="

RESP=$(curl -s -X POST "$BASE/api/v1/cart/items" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"skuId":1,"spuId":1,"name":"Product-SKU1","image":"img.jpg","price":"99.00","quantity":2}')
assert_code "$RESP" "200" "Add to Cart"

RESP=$(curl -s -X POST "$BASE/api/v1/cart/items" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"skuId":1,"spuId":1,"name":"Product-SKU1","image":"img.jpg","price":"99.00","quantity":1}')
assert_code "$RESP" "200" "Same SKU Stack"

RESP=$(curl -s "$BASE/api/v1/cart" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "Get Cart"

RESP=$(curl -s "$BASE/api/v1/cart/count" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "Cart Count"

RESP=$(curl -s -X PUT "$BASE/api/v1/cart/items/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"quantity":5}')
assert_code "$RESP" "200" "Update Qty"

RESP=$(curl -s -X PUT "$BASE/api/v1/cart/items/1/check" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "Toggle Check"

RESP=$(curl -s -X PUT "$BASE/api/v1/cart/items/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"quantity":-1}')
assert_code "$RESP" "35010002" "Negative Qty Blocked"

RESP=$(curl -s -X DELETE "$BASE/api/v1/cart" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "Clear Cart"

# ============================
# 3. Order
# ============================
log_info "=== 3. Order ==="

RESP=$(curl -s -X POST "$BASE/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"receiverName":"Receiver","receiverPhone":"13800001111","receiverAddress":"Beijing Chaoyang","items":[{"skuId":1,"spuId":1,"name":"iPhone15","image":"img.jpg","price":"6999.00","quantity":1}]}')
assert_code "$RESP" "200" "Create Order"
ORDER_ID=$(extract_id "$RESP")
ORDER_NO=$(extract_order_no "$RESP")
log_info "  OrderNo: $ORDER_NO"

RESP=$(curl -s -X POST "$BASE/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"receiverName":"x","receiverPhone":"x","receiverAddress":"x","items":[]}')
assert_code "$RESP" "400" "Empty Items Blocked"

RESP=$(curl -s "$BASE/api/v1/orders" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "User Order List"

RESP=$(curl -s "$BASE/api/v1/orders/$ORDER_ID" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "Order Detail"

RESP=$(curl -s -X PUT "$BASE/api/v1/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "200" "Cancel Order"

RESP=$(curl -s -X PUT "$BASE/api/v1/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID")
assert_code "$RESP" "40010002" "Dup Cancel Blocked"

# Re-create for admin test
RESP=$(curl -s -X POST "$BASE/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"receiverName":"R2","receiverPhone":"13800002222","receiverAddress":"Shanghai Pudong","items":[{"skuId":1,"spuId":1,"name":"iPhone15","image":"img.jpg","price":"6999.00","quantity":1}]}')
assert_code "$RESP" "200" "Re-Create Order"
ORDER_ID2=$(extract_id "$RESP")

RESP=$(curl -s "$BASE/api/v1/admin/orders" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code "$RESP" "200" "Admin Order List"

RESP=$(curl -s -X PUT "$BASE/api/v1/admin/orders/$ORDER_ID2/ship" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code "$RESP" "200" "Admin Ship"

# ============================
# 4. Payment
# ============================
log_info "=== 4. Payment ==="

RESP=$(curl -s -X POST "$BASE/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{"receiverName":"PayTest","receiverPhone":"13800003333","receiverAddress":"Guangzhou Tianhe","items":[{"skuId":1,"spuId":1,"name":"iPhone15","image":"img.jpg","price":"6999.00","quantity":1}]}')
assert_code "$RESP" "200" "Order for Payment"
PAY_ORDER_NO=$(extract_order_no "$RESP")
PAY_ORDER_ID=$(extract_id "$RESP")

RESP=$(curl -s -X POST "$BASE/api/v1/payment/pay" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d "{\"orderNo\":\"$PAY_ORDER_NO\",\"orderId\":$PAY_ORDER_ID,\"amount\":6999.00}")
assert_code "$RESP" "200" "Pay (Mock)"

RESP=$(curl -s -X POST "$BASE/api/v1/payment/pay" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d "{\"orderNo\":\"$PAY_ORDER_NO\",\"orderId\":$PAY_ORDER_ID,\"amount\":6999.00}")
assert_code "$RESP" "50010002" "Dup Pay Blocked"

RESP=$(curl -s "$BASE/api/v1/payment/$PAY_ORDER_NO" \
  -H "Authorization: Bearer $USER_TOKEN")
assert_code "$RESP" "200" "Query Payment"

RESP=$(curl -s "$BASE/api/v1/admin/payment" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
assert_code "$RESP" "200" "Admin Payment List"

RESP=$(curl -s -X POST "$BASE/api/v1/admin/payment/$PAY_ORDER_NO/refund" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"reason":"User refund request"}')
assert_code "$RESP" "200" "Admin Refund"

RESP=$(curl -s -X POST "$BASE/api/v1/admin/payment/$PAY_ORDER_NO/refund" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"reason":"Double refund"}')
assert_code "$RESP" "50010003" "Dup Refund Blocked"

# ============================
# Result
# ============================
echo ""
echo "========================================"
echo -e "  P1 Tests: ${GREEN}$PASS PASS${NC} / ${RED}$FAIL FAIL${NC}"
echo "========================================"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
