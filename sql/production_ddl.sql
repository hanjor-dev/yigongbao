-- 生产流转卡表
CREATE TABLE IF NOT EXISTS production_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    record_no VARCHAR(50) NOT NULL COMMENT '流转卡编号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_code VARCHAR(50) COMMENT '订单编号（冗余）',
    order_type TINYINT NOT NULL COMMENT '订单类型（1=医疗器械，2=非医疗器械）',
    design_package_id BIGINT NOT NULL COMMENT '设计数据包ID',
    design_package_code VARCHAR(50) NOT NULL COMMENT '设计数据包编号',
    production_batch_no VARCHAR(50) NOT NULL COMMENT '生产批号',
    version_no VARCHAR(20) COMMENT '版本号',
    material VARCHAR(100) COMMENT '材质',
    hospital_name VARCHAR(200) COMMENT '医院名称（冗余）',
    hospital_dept_name VARCHAR(100) COMMENT '科室名称（冗余）',
    doctor_name VARCHAR(100) COMMENT '医生姓名（冗余）',
    patient_name VARCHAR(100) COMMENT '患者姓名（冗余）',
    is_urgent TINYINT NOT NULL DEFAULT 0 COMMENT '是否加急（0=否，1=是）',
    is_postal TINYINT NOT NULL DEFAULT 0 COMMENT '是否邮寄（0=否，1=是）',
    expected_delivery_date DATETIME COMMENT '期望交付时间（冗余）',
    processing_center_id BIGINT COMMENT '加工中心ID',
    processing_center_name VARCHAR(100) COMMENT '加工中心名称（冗余）',
    print_device_id BIGINT COMMENT '分配的打印机ID',
    print_device_code VARCHAR(50) COMMENT '打印机编号',
    print_device_name VARCHAR(100) COMMENT '打印机名称（冗余）',
    total_product_count INT NOT NULL DEFAULT 0 COMMENT '产品总数',
    qualified_count INT NOT NULL DEFAULT 0 COMMENT '合格数量',
    unqualified_count INT NOT NULL DEFAULT 0 COMMENT '不合格数量',
    has_redo_product TINYINT NOT NULL DEFAULT 0 COMMENT '是否存在待重做产品（0=否，1=是）',
    status VARCHAR(50) NOT NULL COMMENT '当前状态',
    current_process VARCHAR(50) COMMENT '当前工序',
    qr_code_url VARCHAR(255) COMMENT '流转卡二维码URL',
    pack_device_id BIGINT COMMENT '包装设备ID',
    pack_device_no VARCHAR(50) COMMENT '包装设备编号',
    pack_seal_temperature DECIMAL(5,2) COMMENT '热封温度（℃）',
    pack_seal_time INT COMMENT '热封时间（秒）',
    pack_sterilization_method VARCHAR(100) COMMENT '灭菌方式',
    pack_sterilization_batch_no VARCHAR(50) COMMENT '灭菌批号',
    pack_operator_id BIGINT COMMENT '包装操作人ID',
    pack_operator_name VARCHAR(50) COMMENT '包装操作人姓名',
    pack_time DATETIME COMMENT '包装完成时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_order_id (order_id),
    KEY idx_design_package_id (design_package_id),
    KEY idx_status (status),
    KEY idx_production_batch_no (production_batch_no),
    KEY idx_processing_center_id (processing_center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产流转卡表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_record_no
    ON production_record ((CASE WHEN is_deleted = 0 THEN record_no ELSE NULL END));

-- 生产产品记录表
CREATE TABLE IF NOT EXISTS production_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_record_id BIGINT NOT NULL COMMENT '流转卡ID',
    print_file_id BIGINT NOT NULL COMMENT '打印文件ID',
    product_no VARCHAR(50) NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称',
    spec_name VARCHAR(200) COMMENT '型号规格名称',
    cert_no VARCHAR(200) COMMENT '注册证号',
    material_name VARCHAR(100) COMMENT '材质名称',
    color_name VARCHAR(100) COMMENT '颜色名称',
    file_name VARCHAR(255) COMMENT '打印文件名',
    udi_code VARCHAR(200) COMMENT 'UDI码（仅医疗器械）',
    udi_di VARCHAR(100) COMMENT 'UDI-DI（设备标识符）',
    udi_pi VARCHAR(100) COMMENT 'UDI-PI（生产标识符）',
    udi_generate_time DATETIME COMMENT 'UDI生成时间',
    status VARCHAR(50) NOT NULL COMMENT '产品状态',
    current_process_type VARCHAR(50) COMMENT '当前所在工序',
    qc_result VARCHAR(50) COMMENT '质检结果（pass/redo）',
    qc_remark VARCHAR(500) COMMENT '质检不合格原因',
    qc_time DATETIME COMMENT '质检时间',
    qc_user_id BIGINT COMMENT '质检员ID',
    redo_process_type VARCHAR(50) COMMENT '指定的重做工序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_production_record_id (production_record_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产产品记录表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_no
    ON production_product ((CASE WHEN is_deleted = 0 THEN product_no ELSE NULL END));

-- 工序记录表
CREATE TABLE IF NOT EXISTS production_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_record_id BIGINT NOT NULL COMMENT '流转卡ID',
    process_type VARCHAR(50) NOT NULL COMMENT '工序类型',
    process_name VARCHAR(100) NOT NULL COMMENT '工序名称',
    process_order INT NOT NULL COMMENT '工序顺序',
    device_type VARCHAR(100) COMMENT '关键设备类型',
    device_id BIGINT COMMENT '设备ID',
    device_no VARCHAR(50) COMMENT '设备编号',
    process_params JSON COMMENT '关键参数（JSON格式）',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    operator_id BIGINT COMMENT '操作人员ID',
    operator_name VARCHAR(50) COMMENT '操作人员姓名',
    has_redo TINYINT DEFAULT 0 COMMENT '本工序是否有重做',
    redo_remark VARCHAR(500) COMMENT '重做记录',
    inspection_result VARCHAR(50) COMMENT '工序整体结果',
    inspector_id BIGINT COMMENT '检验员ID',
    inspector_name VARCHAR(50) COMMENT '检验员姓名',
    status VARCHAR(50) NOT NULL COMMENT '工序状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_production_record_id (production_record_id),
    KEY idx_process_type (process_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序记录表';

-- 质检产品记录表
CREATE TABLE IF NOT EXISTS production_process_product_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_process_id BIGINT NOT NULL COMMENT '工序记录ID',
    production_product_id BIGINT NOT NULL COMMENT '产品ID',
    result VARCHAR(50) NOT NULL COMMENT '检验结果（qualified/unqualified）',
    remark VARCHAR(500) COMMENT '不合格原因',
    attempt_no INT NOT NULL DEFAULT 1 COMMENT '尝试次数',
    is_latest TINYINT NOT NULL DEFAULT 1 COMMENT '是否最新记录',
    inspector_id BIGINT COMMENT '检验员ID',
    inspect_time DATETIME COMMENT '检验时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_process_id (production_process_id),
    KEY idx_product_id (production_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检产品记录表';

-- 工序流转记录表
CREATE TABLE IF NOT EXISTS production_process_transfer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_record_id BIGINT NOT NULL COMMENT '流转卡ID',
    from_process_type VARCHAR(50) NOT NULL COMMENT '来源工序代码',
    to_process_type VARCHAR(50) NOT NULL COMMENT '目标工序代码',
    transfer_time DATETIME NOT NULL COMMENT '流转时间',
    scan_user_id BIGINT NOT NULL COMMENT '扫码人ID',
    scan_user_name VARCHAR(50) NOT NULL COMMENT '扫码人姓名',
    handover_user_id BIGINT COMMENT '交接人ID',
    handover_user_name VARCHAR(50) COMMENT '交接人姓名',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_production_record_id (production_record_id),
    KEY idx_transfer_time (transfer_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序流转记录表';

-- 补充 production_record 缺失字段
ALTER TABLE production_record
    ADD COLUMN material_batch_no VARCHAR(80) COMMENT '原材料批号' AFTER pack_time,
    ADD COLUMN print_start_time DATETIME COMMENT '打印开始时间' AFTER material_batch_no,
    ADD COLUMN print_finish_time DATETIME COMMENT '打印完成时间' AFTER print_start_time;
