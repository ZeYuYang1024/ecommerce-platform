-- 会员等级种子数据
-- 需要先执行 init.sql 创建表结构，然后手动生成 Snowflake ID

USE ecommerce_member;

-- 等级种子数据（ID 使用 Snowflake 手动生成或由应用启动时自动插入）
-- 如果使用应用启动自动初始化，此脚本仅作为参考

-- INSERT INTO member_level (id, name, level_code, sort_order, growth_threshold, points_multiplier, birthday_gift_points, discount_rate, free_shipping, priority_support, early_access, icon_url, description, created_at, updated_at)
-- VALUES
-- (1, '普通会员', 'REGULAR', 1, 0, 1.00, 0, 1.00, 0, 0, 0, NULL, '注册即享基础权益', NOW(), NOW()),
-- (2, '银卡会员', 'SILVER',  2, 1000, 1.20, 50, 0.98, 0, 0, 0, NULL, '累计 1000 成长值升级', NOW(), NOW()),
-- (3, '金卡会员', 'GOLD',    3, 5000, 1.50, 100, 0.95, 1, 1, 0, NULL, '累计 5000 成长值升级', NOW(), NOW()),
-- (4, '钻石会员', 'DIAMOND', 4, 20000, 2.00, 200, 0.90, 1, 1, 1, NULL, '累计 20000 成长值升级', NOW(), NOW());
