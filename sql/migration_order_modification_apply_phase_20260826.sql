-- 保存申请提交时订单阶段，用于订单阶段变化后使待审核申请失效
ALTER TABLE order_modification_apply
    ADD COLUMN apply_phase INT NULL COMMENT '提交申请时订单阶段' AFTER apply_time;
ti