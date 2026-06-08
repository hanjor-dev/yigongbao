-- 账户安全功能：登录日志增加IP归属地字段
-- 执行时间：2026-06-08
-- 影响表：sys_login_log

-- 增加归属地字段（使用ALGORITHM=INPLACE减少锁表）
ALTER TABLE sys_login_log
ADD COLUMN location VARCHAR(100) DEFAULT NULL COMMENT 'IP归属地（省市信息）'
AFTER user_agent,
ALGORITHM=INPLACE, LOCK=NONE;

-- 增加索引（用于查询用户登录历史），使用IF NOT EXISTS避免重复创建
CREATE INDEX IF NOT EXISTS idx_user_login_time
ON sys_login_log(user_id, login_time DESC);
