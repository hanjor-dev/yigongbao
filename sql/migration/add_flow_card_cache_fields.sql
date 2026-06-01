-- 添加流转卡Excel缓存相关字段
-- 用于避免每次查询详情都重新生成Excel文件

ALTER TABLE production_record
ADD COLUMN flow_card_file_url VARCHAR(500) COMMENT '流转卡Excel文件URL' AFTER pack_time,
ADD COLUMN flow_card_generate_time DATETIME COMMENT '流转卡Excel生成时间' AFTER flow_card_file_url,
ADD COLUMN content_update_time DATETIME COMMENT '流转卡内容最后更新时间' AFTER flow_card_generate_time;

-- 为现有记录初始化 content_update_time 为 update_time
UPDATE production_record
SET content_update_time = update_time
WHERE content_update_time IS NULL;
