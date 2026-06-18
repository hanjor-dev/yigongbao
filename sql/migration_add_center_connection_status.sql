-- ============================================================
-- 数据迁移脚本：为加工中心表添加连接状态字段
-- 执行时间：2026-06-16
-- 说明：支持加工中心级别的 WebSocket 连接状态管理
-- ============================================================

-- 1. 为加工中心表添加连接状态字段
ALTER TABLE processing_center
ADD COLUMN connection_status TINYINT DEFAULT 0 COMMENT '连接状态（0=离线，1=在线）',
ADD COLUMN last_heartbeat DATETIME COMMENT '最后心跳时间';

-- 2. 添加索引以便快速查询在线中心
CREATE INDEX idx_connection_status ON processing_center(connection_status);
