-- ============================================================
-- P3-A RBAC 种子数据 — 预设角色 + 权限 + 管理员角色分配
-- ============================================================

SET NAMES utf8mb4;
USE ecommerce_auth;

-- 清理旧数据
DELETE FROM admin_user_role;
DELETE FROM role_permission;
DELETE FROM role;
DELETE FROM permission;

-- ============================
-- 1. 预设角色
-- ============================

INSERT IGNORE INTO role (id, name, code, description) VALUES
(1, '超级管理员', 'super_admin', '平台最高权限，管理所有功能和数据'),
(2, '运营人员', 'ops', '管理商品、订单、支付、对账，不能审核商家和管理用户'),
(3, '商家管理员', 'merchant', '管理自己店铺的商品');

-- ============================
-- 2. 预设权限（按模块分组）
-- ============================

-- 数据概览
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
(1, '数据概览', 'dashboard', 'menu', 0, '/dashboard', 'DataAnalysis', 1);

-- 商家管理
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
(10, '商家管理', 'merchants', 'menu', 0, '/merchants', 'Shop', 2),
(11, '商家列表', 'merchants:list', 'menu', 10, '/merchants', NULL, 1),
(12, '商家审核', 'merchants:audit', 'button', 10, NULL, NULL, 2);

-- 商品运营
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
(20, '商品管理', 'products', 'menu', 0, '/products', 'Goods', 3),
(21, '商品列表', 'products:list', 'menu', 20, '/products', NULL, 1),
(22, '商品编辑', 'products:edit', 'button', 20, NULL, NULL, 2),
(23, '类目管理', 'categories', 'menu', 0, '/categories', 'Grid', 4),
(24, '品牌管理', 'brands', 'menu', 0, '/brands', 'Collection', 5),
(25, '评论管理', 'reviews', 'menu', 0, '/reviews', 'ChatDotRound', 6),
(26, '库存管理', 'inventory', 'menu', 0, '/inventory', 'Box', 7);

-- 交易管理
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
(30, '订单管理', 'orders', 'menu', 0, '/orders', 'Document', 8),
(31, '支付管理', 'payments', 'menu', 0, '/payments', 'Money', 9);

-- 财务管理
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
(40, '对账管理', 'reconciliation', 'menu', 0, '/reconciliation', 'RefreshRight', 10),
(41, '日终结算', 'settlement', 'menu', 0, '/settlement', 'TrendCharts', 11);

-- 用户与权限
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
(50, '用户管理', 'users', 'menu', 0, '/users', 'User', 12),
(51, '角色管理', 'roles', 'menu', 0, '/roles', 'Key', 13),
(52, '权限管理', 'permissions', 'menu', 0, '/permissions', 'Lock', 14);

-- ============================
-- 3. 角色分配权限
-- ============================

-- super_admin: 所有权限
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 1, id FROM permission;

-- ops: 商品/交易/财务（不含商家管理 10-12, 用户管理 50-52）
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 2, id FROM permission
WHERE id NOT IN (10, 11, 12, 50, 51, 52);

-- merchant: 仅商品管理
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 3, id FROM permission
WHERE id IN (20, 21, 22);

-- ============================
-- 4. 默认管理员分配 super_admin 角色
-- ============================

INSERT IGNORE INTO admin_user_role (id, admin_user_id, role_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 1, 1;
