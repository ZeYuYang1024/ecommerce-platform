-- ============================================================
-- P0 示例数据：京东风格分类 + 20个商品
-- 执行：mysql -uroot -proot < P0-sample-data.sql
-- 注意：先执行 P0-init.sql 建表，再执行此脚本
-- ============================================================

-- ============================
-- 1. 分类数据（仿京东）
-- ============================

USE ecommerce_product;

-- 一级分类
INSERT INTO category (id, name, parent_id, level, sort) VALUES
(1,  '家用电器',     0, 1, 1),
(2,  '手机 / 数码',  0, 1, 2),
(3,  '电脑 / 办公',  0, 1, 3),
(4,  '家居 / 家具',  0, 1, 4),
(5,  '服饰 / 内衣',  0, 1, 5),
(6,  '美妆 / 护肤',  0, 1, 6),
(7,  '食品 / 生鲜',  0, 1, 7),
(8,  '图书 / 文娱',  0, 1, 8),
(9,  '运动 / 户外',  0, 1, 9),
(10, '汽车 / 用品',  0, 1, 10);

-- 二级分类
INSERT INTO category (id, name, parent_id, level, sort) VALUES
-- 家用电器
(11, '大家电',   1, 2, 1),
(12, '厨房电器', 1, 2, 2),
(13, '生活电器', 1, 2, 3),
-- 手机/数码
(14, '手机',       2, 2, 1),
(15, '摄影摄像',   2, 2, 2),
(16, '智能穿戴',   2, 2, 3),
-- 电脑/办公
(17, '笔记本',     3, 2, 1),
(18, '台式机',     3, 2, 2),
(19, '办公耗材',   3, 2, 3),
-- 家居/家具
(20, '客厅家具', 4, 2, 1),
(21, '卧室家具', 4, 2, 2),
-- 服饰/内衣
(22, '男装', 5, 2, 1),
(23, '女装', 5, 2, 2),
(24, '内衣', 5, 2, 3),
-- 美妆/护肤
(25, '面部护肤', 6, 2, 1),
(26, '彩妆',     6, 2, 2),
-- 食品/生鲜
(27, '休闲零食', 7, 2, 1),
(28, '生鲜果蔬', 7, 2, 2),
(29, '酒水饮料', 7, 2, 3),
-- 图书/文娱
(30, '文学小说', 8, 2, 1),
(31, '少儿图书', 8, 2, 2),
-- 运动/户外
(32, '运动鞋服', 9, 2, 1),
(33, '健身器材', 9, 2, 2),
-- 汽车/用品
(34, '车载电器', 10, 2, 1),
(35, '汽车装饰', 10, 2, 2);

-- ============================
-- 2. 品牌数据
-- ============================

INSERT INTO brand (id, name, description) VALUES
(1, '海尔',   '全球领先的家电品牌'),
(2, '美的',   '智慧生活解决方案'),
(3, '华为',   '中国科技品牌'),
(4, '小米',   '智能科技品牌'),
(5, '苹果',   '美国科技公司'),
(6, '联想',   '全球PC领导品牌'),
(7, '戴尔',   '美国电脑品牌'),
(8, '耐克',   '全球运动品牌'),
(9, '阿迪达斯', '德国运动品牌'),
(10, '欧莱雅', '法国美妆品牌'),
(11, '三只松鼠', '互联网坚果品牌'),
(12, '宜家',  '瑞典家居品牌'),
(13, '索尼',  '日本电子品牌'),
(14, '格力',  '中国空调品牌');

-- ============================
-- 3. 商品数据（20个 SPU + 对应 SKU）
-- ============================

-- 商品1: 冰箱 (家用电器 > 大家电)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(1, '海尔双开门冰箱 BCD-500', 11, 1, '500升大容量，风冷无霜，一级能效', NULL, '<p>海尔双开门冰箱，500L大容量</p><ul><li>风冷无霜</li><li>一级能效</li><li>智能温控</li></ul>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(1, 1, 'BCD-500 银灰色', '{"颜色":"银灰色"}', 3999.00, 4599.00),
(2, 1, 'BCD-500 白色',   '{"颜色":"白色"}',   3899.00, 4499.00);

-- 商品2: 微波炉 (家用电器 > 厨房电器)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(2, '美的智能微波炉 M3-L239', 12, 2, '23L大容量，变频加热，智能菜单', NULL, '<p>美的智能微波炉</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(3, 2, 'M3-L239 黑色', '{"颜色":"黑色"}', 599.00, 699.00),
(4, 2, 'M3-L239 白色', '{"颜色":"白色"}', 579.00, 679.00);

-- 商品3: 空调 (家用电器 > 大家电)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(3, '格力变频空调 KFR-35GW 1.5匹', 11, 14, '新一级能效，变频冷暖，自清洁', NULL, '<p>格力1.5匹变频空调</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(5, 3, 'KFR-35GW 标准款', '{"类型":"标准款"}', 3299.00, 3799.00),
(6, 3, 'KFR-35GW WiFi款', '{"类型":"WiFi智能款"}', 3599.00, 4099.00);

-- 商品4: 手机 (手机/数码 > 手机)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(4, '华为 Mate 70 Pro', 14, 3, '麒麟芯片，卫星通信，XMAGE影像', NULL, '<p>华为旗舰手机 Mate 70 Pro</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(7, 4, 'Mate 70 Pro 12+256GB 雅丹黑', '{"颜色":"雅丹黑","存储":"12+256GB"}', 6499.00, 6999.00),
(8, 4, 'Mate 70 Pro 12+512GB 雅丹黑', '{"颜色":"雅丹黑","存储":"12+512GB"}', 7499.00, 7999.00),
(9, 4, 'Mate 70 Pro 12+512GB 白沙银', '{"颜色":"白沙银","存储":"12+512GB"}', 7499.00, 7999.00);

-- 商品5: 手机 (手机/数码 > 手机)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(5, '小米 15 Ultra', 14, 4, '骁龙8Gen4，徕卡光学，120W快充', NULL, '<p>小米旗舰手机 15 Ultra</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(10, 5, '15 Ultra 12+256GB 黑色', '{"颜色":"黑色","存储":"12+256GB"}', 4999.00, 5299.00),
(11, 5, '15 Ultra 16+512GB 白色', '{"颜色":"白色","存储":"16+512GB"}', 5799.00, 6099.00);

-- 商品6: 蓝牙耳机 (手机/数码 > 智能穿戴)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(6, '小米 Buds 5 Pro', 16, 4, '主动降噪，Hi-Res音质，36小时续航', NULL, '<p>小米旗舰无线耳机</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(12, 6, 'Buds 5 Pro 黑色', '{"颜色":"黑色"}', 799.00, 899.00),
(13, 6, 'Buds 5 Pro 白色', '{"颜色":"白色"}', 799.00, 899.00);

-- 商品7: 笔记本电脑 (电脑/办公 > 笔记本)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(7, '联想 ThinkPad X1 Carbon', 17, 6, '14英寸商务旗舰，i7处理器，1TB固态', NULL, '<p>ThinkPad X1 Carbon Gen12</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(14, 7, 'X1C i7-155H 16GB+512GB', '{"配置":"i7-155H 16+512"}', 9999.00, 10999.00),
(15, 7, 'X1C i7-155H 32GB+1TB',  '{"配置":"i7-155H 32+1T"}',  11999.00, 12999.00);

-- 商品8: 笔记本电脑 (电脑/办公 > 笔记本)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(8, '戴尔 XPS 14', 17, 7, '14英寸OLED屏，Ultra9处理器', NULL, '<p>戴尔 XPS 14 2025款</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(16, 8, 'XPS14 Ultra9 16GB+512GB', '{"配置":"Ultra9 16+512"}', 10999.00, 11999.00),
(17, 8, 'XPS14 Ultra9 32GB+1TB',  '{"配置":"Ultra9 32+1T"}',  12999.00, 13999.00);

-- 商品9: 主板 (电脑/办公 > 台式机)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(9, '华硕 ROG MAXIMUS Z890 HERO', 18, NULL, 'Z890芯片组，LGA1851，DDR5', NULL, '<p>ROG旗舰主板</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(18, 9, 'Z890 HERO 标准版', '{"版本":"标准版"}', 4999.00, 5499.00);

-- 商品10: 沙发 (家居/家具 > 客厅家具)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(10, '北欧风布艺沙发 三人位', 20, 12, '简约北欧风，高回弹海绵，实木框架', NULL, '<p>北欧简约三人位沙发</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(19, 10, '三人位 浅灰色', '{"颜色":"浅灰色","规格":"三人位"}', 2999.00, 3599.00),
(20, 10, '三人位 深蓝色', '{"颜色":"深蓝色","规格":"三人位"}', 2999.00, 3599.00);

-- 商品11: 床垫 (家居/家具 > 卧室家具)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(11, '宜家独立弹簧床垫 1.8m', 21, 12, '独立袋装弹簧，天然乳胶层，透气面料', NULL, '<p>宜家高端床垫</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(21, 11, '1.8m 标准款', '{"规格":"1.8m×2.0m","厚度":"22cm"}', 3999.00, 4999.00),
(22, 11, '1.8m 加厚款', '{"规格":"1.8m×2.0m","厚度":"28cm"}', 4999.00, 5999.00);

-- 商品12: 男士夹克 (服饰/内衣 > 男装)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(12, '商务休闲立领夹克', 22, NULL, '含棉面料，立体剪裁，商务休闲两穿', NULL, '<p>春季商务休闲夹克</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(23, 12, '夹克 黑色 M', '{"颜色":"黑色","尺码":"M"}', 459.00, 599.00),
(24, 12, '夹克 黑色 L', '{"颜色":"黑色","尺码":"L"}', 459.00, 599.00),
(25, 12, '夹克 卡其色 L', '{"颜色":"卡其色","尺码":"L"}', 459.00, 599.00);

-- 商品13: 女士连衣裙 (服饰/内衣 > 女装)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(13, '法式复古碎花连衣裙', 23, NULL, 'V领设计，收腰显瘦，雪纺面料', NULL, '<p>夏季法式碎花连衣裙</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(26, 13, '连衣裙 碎花蓝 S', '{"颜色":"碎花蓝","尺码":"S"}', 299.00, 399.00),
(27, 13, '连衣裙 碎花蓝 M', '{"颜色":"碎花蓝","尺码":"M"}', 299.00, 399.00),
(28, 13, '连衣裙 碎花粉 M', '{"颜色":"碎花粉","尺码":"M"}', 299.00, 399.00);

-- 商品14: 运动鞋男 (运动/户外 > 运动鞋服)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(14, '耐克 Air Zoom Pegasus 42', 32, 8, '全掌Zoom气垫，Flyknit鞋面，轻量缓震', NULL, '<p>耐克飞马42代跑鞋</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(29, 14, 'Pegasus42 黑白 40', '{"颜色":"黑白","尺码":"40"}', 899.00, 999.00),
(30, 14, 'Pegasus42 黑白 42', '{"颜色":"黑白","尺码":"42"}', 899.00, 999.00),
(31, 14, 'Pegasus42 全黑 42', '{"颜色":"全黑","尺码":"42"}', 899.00, 999.00);

-- 商品15: 运动鞋女 (运动/户外 > 运动鞋服)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(15, '阿迪达斯 Ultraboost 5.0 女款', 32, 9, 'Boost中底，Primeknit编织，运动休闲', NULL, '<p>Adidas UB5.0女款跑鞋</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(32, 15, 'UB5.0 白色 37', '{"颜色":"白色","尺码":"37"}', 999.00, 1199.00),
(33, 15, 'UB5.0 白色 38', '{"颜色":"白色","尺码":"38"}', 999.00, 1199.00);

-- 商品16: 防晒霜 (美妆/护肤 > 面部护肤)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(16, '欧莱雅小金管防晒霜 SPF50+', 25, 10, '高倍防晒，清爽不油腻，麦色滤光科技', NULL, '<p>欧莱雅小金管防晒</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(34, 16, '小金管 30ml', '{"规格":"30ml"}', 129.00, 159.00),
(35, 16, '小金管 50ml', '{"规格":"50ml"}', 189.00, 229.00);

-- 商品17: 口红 (美妆/护肤 > 彩妆)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(17, '欧莱雅小钢笔唇釉', 26, 10, '哑光雾面，持久不脱色，轻薄显色', NULL, '<p>欧莱雅小钢笔唇釉</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(36, 17, '唇釉 #130 枫叶红', '{"色号":"#130 枫叶红"}', 99.00, 129.00),
(37, 17, '唇釉 #145 豆沙粉', '{"色号":"#145 豆沙粉"}', 99.00, 129.00);

-- 商品18: 坚果礼盒 (食品/生鲜 > 休闲零食)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(18, '三只松鼠坚果大礼包 1688g', 27, 11, '9袋混合坚果，年货送礼，每日坚果', NULL, '<p>三只松鼠年货坚果大礼包</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(38, 18, '坚果礼包 1688g', '{"规格":"1688g"}', 168.00, 228.00);

-- 商品19: 华为手表 (手机/数码 > 智能穿戴)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(19, '华为 WATCH GT 5 Pro', 16, 3, '钛合金表壳，14天续航，ECG心电分析', NULL, '<p>华为智能手表GT5 Pro</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(39, 19, 'GT5 Pro 钛灰色', '{"颜色":"钛灰色","表带":"氟橡胶"}', 2788.00, 2988.00),
(40, 19, 'GT5 Pro 钛灰色 皮表带', '{"颜色":"钛灰色","表带":"真皮"}', 2988.00, 3188.00);

-- 商品20: 键盘 (电脑/办公 > 办公耗材)
INSERT INTO spu (id, name, category_id, brand_id, description, main_image, detail, status) VALUES
(20, '罗技 MX Keys S 无线键盘', 19, NULL, '全尺寸，智能背光，多设备切换，USB-C充电', NULL, '<p>罗技旗舰办公键盘</p>', 1);
INSERT INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(41, 20, 'MX Keys S 石墨黑', '{"颜色":"石墨黑"}', 799.00, 899.00),
(42, 20, 'MX Keys S 珍珠白', '{"颜色":"珍珠白"}', 799.00, 899.00);

-- ============================
-- 4. 库存数据（每个 SKU 初始库存）
-- ============================

USE ecommerce_inventory;

INSERT INTO stock (id, sku_id, total_stock, locked_stock, available_stock, version) VALUES
-- 冰箱
(1,  1,  200, 0, 200, 0), (2,  2,  150, 0, 150, 0),
-- 微波炉
(3,  3,  500, 0, 500, 0), (4,  4,  450, 0, 450, 0),
-- 空调
(5,  5,  100, 0, 100, 0), (6,  6,  80,  0, 80,  0),
-- Mate70 Pro
(7,  7,  300, 0, 300, 0), (8,  8,  200, 0, 200, 0), (9,  9,  150, 0, 150, 0),
-- 小米15
(10, 10, 400, 0, 400, 0), (11, 11, 300, 0, 300, 0),
-- Buds5
(12, 12, 600, 0, 600, 0), (13, 13, 500, 0, 500, 0),
-- ThinkPad
(14, 14, 80,  0, 80,  0), (15, 15, 50,  0, 50,  0),
-- XPS14
(16, 16, 60,  0, 60,  0), (17, 17, 30,  0, 30,  0),
-- 主板
(18, 18, 40,  0, 40,  0),
-- 沙发
(19, 19, 30,  0, 30,  0), (20, 20, 25,  0, 25,  0),
-- 床垫
(21, 21, 50,  0, 50,  0), (22, 22, 30,  0, 30,  0),
-- 夹克
(23, 23, 200, 0, 200, 0), (24, 24, 250, 0, 250, 0), (25, 25, 180, 0, 180, 0),
-- 连衣裙
(26, 26, 150, 0, 150, 0), (27, 27, 200, 0, 200, 0), (28, 28, 180, 0, 180, 0),
-- 耐克
(29, 29, 300, 0, 300, 0), (30, 30, 350, 0, 350, 0), (31, 31, 250, 0, 250, 0),
-- Adidas
(32, 32, 200, 0, 200, 0), (33, 33, 220, 0, 220, 0),
-- 防晒
(34, 34, 800, 0, 800, 0), (35, 35, 600, 0, 600, 0),
-- 口红
(36, 36, 500, 0, 500, 0), (37, 37, 450, 0, 450, 0),
-- 坚果
(38, 38, 1000,0, 1000,0),
-- GT5
(39, 39, 150, 0, 150, 0), (40, 40, 100, 0, 100, 0),
-- 键盘
(41, 41, 300, 0, 300, 0), (42, 42, 280, 0, 280, 0);
