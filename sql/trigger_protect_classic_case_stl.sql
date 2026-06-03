-- ============================================================
-- 经典案例STL模型文件保护触发器
-- 功能：防止删除经典案例订单的STL重建模型文件
-- 作用时机：在 file_detail 表执行逻辑删除（UPDATE is_deleted=1）前触发
-- 保护条件：
--   1. 文件扩展名为 .stl
--   2. 文件路径包含 classic-cases/（经典案例专用目录）
-- ============================================================

DELIMITER $$

DROP TRIGGER IF EXISTS prevent_delete_classic_case_stl$$

CREATE TRIGGER prevent_delete_classic_case_stl
BEFORE UPDATE ON file_detail
FOR EACH ROW
BEGIN
  -- 检查是否是逻辑删除操作（is_deleted 从 0 改为 1）
  -- 且文件是STL格式且路径包含 classic-cases/
  IF OLD.is_deleted = 0
     AND NEW.is_deleted = 1
     AND LOWER(NEW.ext) = 'stl'
     AND NEW.path LIKE '%classic-cases/%' THEN

    -- 阻止删除，抛出错误
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = '经典案例的STL重建模型文件受数据库级保护，不允许删除';
  END IF;
END$$

DELIMITER ;
