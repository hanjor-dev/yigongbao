-- 为设计工单增加独立的设计师备注字段
ALTER TABLE order_main
    ADD COLUMN designer_remark TEXT NULL COMMENT '设计师备注' AFTER data_evaluation_opinion;
