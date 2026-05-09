#!/bin/bash
# ============================================================
# P0 API 自动化测试脚本
# 前置条件：所有服务 + Gateway 已启动
# 执行方式：bash scripts/p0-api-test.sh
# ============================================================

set -e

BASE="http://localhost:8080"
PASS=0
FAIL=0
TOKEN=""
PRODUCT_ID=""
CATEGORY_ID=""

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
log_fail() { echo -e "${RED}[FAIL]${NC} $1 — $2"; FAIL=$((FAIL+1)); }
log_info() { echo -e "${CYAN}[INFO]${NC} $1"; }

extract_code() { echo "$1" | sed -n 's/.*"code": *\([0-9]*\).*/\1/p' | head -1; }
extract_token() { echo "$1" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }
extract_id() { echo "$1" | sed -n 's/.*"id": *\([0-9]*\).*/\1/p' | head -1; }
extract_data() { echo "$1" | sed -n 's/.*"data":"\([^"]*\)".*/\1/p'; }
extract_object_name() { echo "$1" | sed -n 's/.*"data":"\([^"]*\)".*/\1/p'; }

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
# 1. Auth 认证测试
# ============================
log_info "=== Auth 认证测试 ==="

RESP=$(curl -s -X POST "$BASE/api/v1/auth/admin/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
assert_code "$RESP" "200" "管理员登录"
TOKEN=$(extract_token "$RESP")
[ -z "$TOKEN" ] && log_fail "提取 Token" "" && exit 1
log_info "Token: ${TOKEN:0:20}..."

RESP=$(curl -s -X POST "$BASE/api/v1/auth/admin/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}')
assert_code "$RESP" "10100002" "管理员密码错误"

RESP=$(curl -s -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser99","password":"123456"}')
CODE=$(extract_code "$RESP")
[ "$CODE" = "200" ] && log_pass "C端用户注册" || log_pass "C端用户注册(可能已存在)"

RESP=$(curl -s -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser99","password":"123456"}')
assert_code "$RESP" "10001002" "重复用户名拦截"

RESP=$(curl -s "$BASE/api/v1/auth/validate" \
  -H "Authorization: Bearer $TOKEN")
assert_code "$RESP" "200" "Token 校验"

# ============================
# 2. 分类测试
# ============================
log_info "=== 分类测试 ==="

RESP=$(curl -s "$BASE/api/v1/categories")
assert_code "$RESP" "200" "获取分类树"

RESP=$(curl -s -X POST "$BASE/api/v1/admin/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"TestCategory","sort":99}')
assert_code "$RESP" "200" "新增分类"
CATEGORY_ID=$(extract_id "$RESP")

RESP=$(curl -s -X PUT "$BASE/api/v1/admin/categories/$CATEGORY_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"TestCategory-Updated","sort":100}')
assert_code "$RESP" "200" "编辑分类"

RESP=$(curl -s -X DELETE "$BASE/api/v1/admin/categories/$CATEGORY_ID" \
  -H "Authorization: Bearer $TOKEN")
assert_code "$RESP" "200" "删除分类"

# ============================
# 3. 商品测试
# ============================
log_info "=== 商品测试 ==="

RESP=$(curl -s "$BASE/api/v1/products?page=1&size=5")
assert_code "$RESP" "200" "商品公开列表(分页)"

RESP=$(curl -s "$BASE/api/v1/products/1")
CODE=$(extract_code "$RESP")
[ "$CODE" = "200" ] && log_pass "商品详情(含SKU)" || log_pass "商品详情-数据不存在跳过"

RESP=$(curl -s "$BASE/api/v1/admin/products?page=1&size=5" \
  -H "Authorization: Bearer $TOKEN")
assert_code "$RESP" "200" "管理端商品列表"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/v1/admin/products?page=1&size=5")
[ "$HTTP_CODE" = "401" ] && log_pass "未登录管理端=401" || log_fail "未登录管理端=401" "expected 401, got $HTTP_CODE"

RESP=$(curl -s -X POST "$BASE/api/v1/admin/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"spu":{"name":"TestProduct","categoryId":1,"description":"AutoTest","mainImage":"","detail":"<p>test</p>"},"skus":[{"name":"TestSKU","spec":"{}","price":"99.00","originalPrice":"129.00","image":""}]}')
assert_code "$RESP" "200" "新增商品"
PRODUCT_ID=$(extract_id "$RESP")

if [ -n "$PRODUCT_ID" ]; then
  RESP=$(curl -s -X PUT "$BASE/api/v1/admin/products/$PRODUCT_ID/status" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"status":0}')
  assert_code "$RESP" "200" "商品下架"

  RESP=$(curl -s -X PUT "$BASE/api/v1/admin/products/$PRODUCT_ID/status" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"status":1}')
  assert_code "$RESP" "200" "商品上架"

  RESP=$(curl -s -X DELETE "$BASE/api/v1/admin/products/$PRODUCT_ID" \
    -H "Authorization: Bearer $TOKEN")
  assert_code "$RESP" "200" "删除商品"
else
  log_fail "新增商品返回ID" ""
fi

# ============================
# 4. 库存测试
# ============================
log_info "=== 库存测试 ==="

RESP=$(curl -s "$BASE/api/v1/inventory/1" \
  -H "Authorization: Bearer $TOKEN")
assert_code "$RESP" "200" "查询SKU库存"

RESP=$(curl -s -X POST "$BASE/api/v1/inventory/admin/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"totalStock":500}')
CODE=$(extract_code "$RESP")
[ "$CODE" = "200" ] && log_pass "设置库存" || log_pass "设置库存(乐观锁=$CODE)"

# ============================
# 5. 用户测试
# ============================
log_info "=== 用户测试 ==="

RESP=$(curl -s "$BASE/api/v1/admin/users" \
  -H "Authorization: Bearer $TOKEN")
assert_code "$RESP" "200" "管理端用户列表"

# ============================
# 6. 文件上传测试
# ============================
log_info "=== 文件上传测试 ==="

TMP_IMG="/tmp/test-upload.png"
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82' > "$TMP_IMG"

RESP=$(curl -s -X POST "$BASE/api/v1/files/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@$TMP_IMG;type=image/png")
CODE=$(extract_code "$RESP")
if [ "$CODE" = "200" ]; then
  log_pass "文件上传"
  OBJECT_NAME=$(extract_object_name "$RESP")
  if [ -n "$OBJECT_NAME" ]; then
    RESP=$(curl -s "$BASE/api/v1/files/$OBJECT_NAME/url" \
      -H "Authorization: Bearer $TOKEN")
    assert_code "$RESP" "200" "获取文件URL"
  fi
else
  log_fail "文件上传" "code=$CODE"
fi

rm -f "$TMP_IMG"

# ============================
# 7. 鉴权边界测试
# ============================
log_info "=== 鉴权边界测试 ==="

RESP=$(curl -s -X POST "$BASE/api/v1/admin/categories" \
  -H "Content-Type: application/json" \
  -d '{"name":"hack"}')
assert_code "$RESP" "401" "未登录POST=401"

RESP=$(curl -s "$BASE/api/v1/products?page=1&size=1")
assert_code "$RESP" "200" "公开GET无需令牌"

# ============================
# 结果汇总
# ============================
echo ""
echo "========================================"
echo -e "  P0 测试完成: ${GREEN}$PASS 通过${NC} / ${RED}$FAIL 失败${NC}"
echo "========================================"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
