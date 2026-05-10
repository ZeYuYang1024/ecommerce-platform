-- ============================================================
-- E2E 测试数据
-- 确保测试用户和管理员存在，商品数据可用于 E2E 测试
-- 前置：所有服务已启动，P0/P1/P2 SQL 已执行
-- ============================================================

-- ============================
-- 1. 测试管理员 (密码 admin123 = MD5 0192023a7bbd73250516f069df18b500)
-- ============================

USE ecommerce_auth;

INSERT IGNORE INTO admin_user (id, username, password, status)
VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', 1);

-- ============================
-- 2. 测试用户 (密码 test123 = MD5 cc03e747a6afbbcbf8be7668acfebee5)
-- ============================

INSERT IGNORE INTO user (id, username, password, phone, status)
VALUES (10001, 'e2euser', 'cc03e747a6afbbcbf8be7668acfebee5', '13800000000', 1);

-- ============================
-- 3. 测试商品
-- ============================

USE ecommerce_product;

-- 分类
INSERT IGNORE INTO category (id, name, parent_id, level, sort) VALUES
(1, '手机数码', 0, 1, 1),
(2, '电脑办公', 0, 1, 2),
(3, '手机', 1, 2, 1),
(4, '平板', 1, 2, 2),
(5, '笔记本', 2, 2, 1);

-- 品牌
INSERT IGNORE INTO brand (id, name, description) VALUES
(1, 'Apple', '苹果公司'),
(2, 'Huawei', '华为技术'),
(3, 'Xiaomi', '小米科技');

-- SPU + SKU
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, main_image, status, avg_rating, review_count)
VALUES (1001, 'iPhone 15 Pro', 3, 1, 'A17 Pro 芯片，钛金属设计', '', 1, 4.8, 256);

INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price)
VALUES (1011, 1001, 'iPhone 15 Pro 128GB 黑色', '{"颜色":"黑色","存储":"128GB"}', 6999.00, 7999.00),
       (1012, 1001, 'iPhone 15 Pro 256GB 白色', '{"颜色":"白色","存储":"256GB"}', 7999.00, NULL);

INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, main_image, status)
VALUES (1002, 'MacBook Air M4', 5, 1, 'M4 芯片，超轻薄', '', 1);

INSERT IGNORE INTO sku (id, spu_id, name, spec, price)
VALUES (1021, 1002, 'MacBook Air M4 16GB+256GB', '{"内存":"16GB","存储":"256GB"}', 8999.00),
       (1022, 1002, 'MacBook Air M4 16GB+512GB', '{"内存":"16GB","存储":"512GB"}', 10499.00);

-- 库存
USE ecommerce_inventory;

INSERT IGNORE INTO stock (id, sku_id, total_stock, locked_stock, available_stock, version)
VALUES (1, 1011, 100, 0, 100, 0),
       (2, 1012, 50, 0, 50, 0),
       (3, 1021, 30, 0, 30, 0),
       (4, 1022, 20, 0, 20, 0);

-- ============================
-- 4. 测试收货地址
-- ============================

USE ecommerce_user;

INSERT IGNORE INTO address (id, user_id, receiver_name, receiver_phone, province, city, district, detail, is_default)
VALUES (20001, 10001, 'E2E测试用户', '13800000000', '北京市', '北京市', '朝阳区', '望京测试地址100号', 1);
