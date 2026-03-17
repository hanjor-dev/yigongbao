-- ============================================================
-- 医工宝系统初始化数据
-- 包含字典数据初始化语句
-- ============================================================

-- ------------------------------------------------------------
-- 机构类型（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (1, 0, '1', '机构类型', NULL, 1, 1, 1);

-- 机构类型（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(2, 1, '1.1', '生产企业', 'production', 2, 1, 1),
(3, 1, '1.2', '经销商', 'distributor', 2, 2, 1),
(4, 1, '1.3', '医疗机构', 'medical', 2, 3, 1),
(5, 1, '1.4', '其他', 'other', 2, 4, 1);

-- ------------------------------------------------------------
-- 机构编码前缀（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (6, 0, '2', '机构编码前缀', NULL, 1, 2, 1);

-- 机构编码前缀（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(7, 6, '2.1', '生产企业编码前缀', 'ORG-P-', 2, 1, 1),
(8, 6, '2.2', '经销商编码前缀', 'ORG-D-', 2, 2, 1),
(9, 6, '2.3', '医疗机构编码前缀', 'ORG-H-', 2, 3, 1),
(10, 6, '2.4', '其他编码前缀', 'ORG-O-', 2, 4, 1);

-- ------------------------------------------------------------
-- 医院等级（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (11, 0, '3', '医院等级', NULL, 1, 3, 1);

-- 医院等级（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(12, 11, '3.1', '三甲', '1', 2, 1, 1),
(13, 11, '3.2', '三乙', '2', 2, 2, 1),
(14, 11, '3.3', '二甲', '3', 2, 3, 1),
(15, 11, '3.4', '二乙', '4', 2, 4, 1),
(16, 11, '3.5', '其他', '5', 2, 5, 1);

-- ------------------------------------------------------------
-- 医院类型（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (17, 0, '4', '医院类型', NULL, 1, 4, 1);

-- 医院类型（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(18, 17, '4.1', '综合', '1', 2, 1, 1),
(19, 17, '4.2', '专科', '2', 2, 2, 1);

-- ------------------------------------------------------------
-- 代理产品线（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (20, 0, '5', '代理产品线', NULL, 1, 5, 1);

-- 代理产品线（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(21, 20, '5.1', '医疗器械', 'medical_device', 2, 1, 1),
(22, 20, '5.2', '药品', 'drug', 2, 2, 1),
(23, 20, '5.3', '耗材', 'consumable', 2, 3, 1),
(24, 20, '5.4', '设备', 'equipment', 2, 4, 1);

-- ============================================================
-- 地区数据（sys_area）导入说明
-- 表结构与 https://github.com/kakuilan/china_area_mysql 的 cnarea_2023 兼容
-- 推荐步骤：
-- 1. 在库中先执行 sql/ddl.sql 创建 sys_area 表
-- 2. 下载并解压 cnarea_2023.sql.zip，将 cnarea_2023 表导入同一库（或另一库后同库再导）
-- 3. 仅需省/市/区三级时，可从 cnarea_2023 导入到 sys_area（补全项目公共字段）：
-- ============================================================
-- INSERT INTO sys_area (level, parent_code, area_code, zip_code, city_code, name, short_name, merger_name, pinyin, lng, lat, status, create_time, update_time, create_by, update_by, is_deleted)
-- SELECT level, parent_code, area_code, zip_code, city_code, name, short_name, merger_name, pinyin, lng, lat, 1, NOW(), NOW(), NULL, NULL, 0
-- FROM cnarea_2023
-- WHERE level IN (1, 2, 3);
