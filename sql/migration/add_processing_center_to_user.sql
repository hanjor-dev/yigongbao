-- 为 sys_user 表添加加工中心关联字段
-- 用途：生产员角色绑定加工中心，只能操作自己绑定中心的打印设备

ALTER TABLE sys_user
ADD COLUMN center_id BIGINT NULL COMMENT '所属加工中心ID（生产员角色专用）' AFTER role_code,
ADD COLUMN center_name VARCHAR(64) NULL COMMENT '所属加工中心名称（冗余字段，生产员角色专用）' AFTER center_id;

-- 添加索引以提升查询性能
CREATE INDEX idx_sys_user_center_id ON sys_user(center_id);
