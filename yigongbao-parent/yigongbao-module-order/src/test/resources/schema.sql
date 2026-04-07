-- ==================== 订单模块 schema.sql ====================
-- 用于 H2 内存数据库测试

-- 订单草稿表
CREATE TABLE order_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT COMMENT '操作员ID（创建人）',
    order_type INT COMMENT '订单类型：1-医疗器械，2-非医疗器械',
    needs_physical_delivery INT NOT NULL DEFAULT 1 COMMENT '是否需要实体交付：0-不需要，1-需要',
    business_type VARCHAR(50) COMMENT '业务类型（字典 dict_code）',
    org_id BIGINT COMMENT '提单机构ID',
    org_name VARCHAR(128) COMMENT '提单机构名称',
    operator_name VARCHAR(64) COMMENT '操作员姓名',
    operator_phone VARCHAR(20) COMMENT '操作员电话',
    hospital_id BIGINT COMMENT '医院ID',
    hospital_name VARCHAR(128) COMMENT '医院名称',
    area_id BIGINT COMMENT '地区ID（冗余自医院）',
    area_name VARCHAR(64) COMMENT '地区名称（冗余自医院）',
    full_area_name VARCHAR(256) COMMENT '完整地区路径名称（冗余自医院）',
    dept_id BIGINT COMMENT '科室ID',
    dept_name VARCHAR(128) COMMENT '科室名称',
    doctor_id BIGINT COMMENT '医生ID',
    doctor_name VARCHAR(64) COMMENT '医生姓名',
    doctor_phone VARCHAR(20) COMMENT '医生电话',
    patient_name VARCHAR(64) COMMENT '患者姓名',
    patient_age INT COMMENT '患者年龄',
    patient_gender VARCHAR(10) COMMENT '患者性别（字典 dict_code）',
    is_urgent INT DEFAULT 0 COMMENT '是否加急：0-否，1-是',
    is_postal INT DEFAULT 0 COMMENT '是否邮寄：0-否，1-是',
    postal_address VARCHAR(512) COMMENT '邮寄地址',
    expected_delivery_date TIMESTAMP COMMENT '期望交付时间',
    expires_at TIMESTAMP COMMENT '过期时间',
    status INT DEFAULT 1 COMMENT '状态：1-有效，2-已提交，3-已过期',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);

-- 订单草稿明细表
CREATE TABLE order_item_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    draft_id BIGINT NOT NULL COMMENT '草稿ID',
    body_part_id BIGINT COMMENT '部位ID',
    body_part_name VARCHAR(128) COMMENT '部位名称',
    project_id BIGINT COMMENT '重建项目ID',
    project_name VARCHAR(128) COMMENT '重建项目名称',
    project_estimated_hours DECIMAL(8,2) COMMENT '预计耗时（小时）',
    project_desc VARCHAR(512) COMMENT '项目说明',
    forming_requirement VARCHAR(512) COMMENT '成形需求',
    other_requirement VARCHAR(512) COMMENT '其他要求',
    sort_order INT DEFAULT 1 COMMENT '排序序号',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);

-- 订单主表
CREATE TABLE order_main (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_code VARCHAR(64) COMMENT '订单编号',
    order_type INT COMMENT '订单类型：1-医疗器械，2-非医疗器械',
    needs_physical_delivery INT NOT NULL DEFAULT 1 COMMENT '是否需要实体交付：0-不需要，1-需要',
    business_type VARCHAR(50) COMMENT '业务类型（字典 dict_code）',
    org_id BIGINT COMMENT '提单机构ID',
    org_name VARCHAR(128) COMMENT '提单机构名称',
    operator_id BIGINT COMMENT '操作员ID（创建人）',
    operator_name VARCHAR(64) COMMENT '操作员姓名',
    operator_phone VARCHAR(20) COMMENT '操作员电话',
    hospital_id BIGINT COMMENT '医院ID',
    hospital_name VARCHAR(128) COMMENT '医院名称',
    area_id BIGINT COMMENT '地区ID（冗余自医院）',
    area_name VARCHAR(64) COMMENT '地区名称（冗余自医院）',
    full_area_name VARCHAR(256) COMMENT '完整地区路径名称（冗余自医院）',
    dept_id BIGINT COMMENT '科室ID',
    dept_name VARCHAR(128) COMMENT '科室名称',
    doctor_id BIGINT COMMENT '医生ID',
    doctor_name VARCHAR(64) COMMENT '医生姓名',
    doctor_phone VARCHAR(20) COMMENT '医生电话',
    patient_name VARCHAR(64) COMMENT '患者姓名',
    patient_age INT COMMENT '患者年龄',
    patient_gender VARCHAR(10) COMMENT '患者性别（字典 dict_code）',
    is_urgent INT DEFAULT 0 COMMENT '是否加急：0-否，1-是',
    is_postal INT DEFAULT 0 COMMENT '是否邮寄：0-否，1-是',
    postal_address VARCHAR(512) COMMENT '邮寄地址',
    expected_delivery_date TIMESTAMP COMMENT '期望交付时间',
    design_start_time TIMESTAMP COMMENT '设计开始时间',
    design_submit_time TIMESTAMP COMMENT '设计提交时间',
    user_confirm_time TIMESTAMP COMMENT '用户确认时间',
    actual_complete_time TIMESTAMP COMMENT '实际完成时间',
    phase INT DEFAULT 1 COMMENT '当前阶段：1-订单，2-设计，3-打印，4-后处理，5-质检，6-仓储，7-确认，8-完成',
    status INT DEFAULT 20 COMMENT '当前状态',
    current_handler_id BIGINT COMMENT '当前处理人ID',
    current_handler_name VARCHAR(64) COMMENT '当前处理人姓名',
    designer_id BIGINT COMMENT '设计师ID',
    designer_name VARCHAR(100) COMMENT '设计师姓名（冗余）',
    producer_id BIGINT COMMENT '生产员ID',
    audit_remark VARCHAR(512) COMMENT '审核备注',
    design_review_remark VARCHAR(512) COMMENT '设计审核备注',
    estimated_cost DECIMAL(10,2) COMMENT '预估费用',
    data_evaluation_opinion TEXT COMMENT '影像数据评估意见',
    version INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0,
    KEY idx_order_code (order_code)
);

-- 订单明细表
CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_code VARCHAR(64) COMMENT '订单编号',
    body_part_id BIGINT COMMENT '部位ID',
    body_part_name VARCHAR(128) COMMENT '部位名称',
    project_id BIGINT COMMENT '重建项目ID',
    project_name VARCHAR(128) COMMENT '重建项目名称',
    project_estimated_hours DECIMAL(8,2) COMMENT '预计耗时（小时）',
    project_desc VARCHAR(512) COMMENT '项目说明',
    forming_requirement VARCHAR(512) COMMENT '成形需求',
    other_requirement VARCHAR(512) COMMENT '其他要求',
    sort_order INT DEFAULT 1 COMMENT '排序序号',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);

-- 订单文件关联表
CREATE TABLE order_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT COMMENT '订单ID',
    order_code VARCHAR(64) COMMENT '订单编号',
    file_id VARCHAR(64) COMMENT '文件ID',
    file_category VARCHAR(50) COMMENT '文件类别（字典 dict_code）',
    package_no VARCHAR(64) COMMENT '数据包编号',
    order_item_id BIGINT COMMENT '关联的订单明细ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);

-- 订单流程状态历史表
CREATE TABLE order_flow_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT COMMENT '订单ID',
    order_code VARCHAR(64) COMMENT '订单编号',
    phase INT COMMENT '变更时阶段',
    from_status INT COMMENT '变更前状态',
    to_status INT COMMENT '变更后状态',
    action VARCHAR(50) COMMENT '触发动作（如 SUBMIT_ORDER、DATA_AUDIT_PASS）',
    action_name VARCHAR(100) COMMENT '动作名称',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    remark VARCHAR(512) COMMENT '备注（如驳回原因）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);

-- 订单修改申请表（P1）
CREATE TABLE order_modify_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_code VARCHAR(64) COMMENT '订单编号',
    apply_type VARCHAR(32) COMMENT '申请类型',
    apply_reason VARCHAR(512) COMMENT '申请原因',
    old_value VARCHAR(512) COMMENT '原值',
    new_value VARCHAR(512) COMMENT '新值',
    applicant_id BIGINT COMMENT '申请人ID',
    applicant_name VARCHAR(64) COMMENT '申请人姓名',
    auditor_id BIGINT COMMENT '审核人ID',
    auditor_name VARCHAR(64) COMMENT '审核人姓名',
    status INT DEFAULT 0 COMMENT '状态：0-待审核，1-通过，2-驳回',
    audit_remark VARCHAR(512) COMMENT '审核备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);

-- 订单修改记录表（P1）
CREATE TABLE order_modification_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT COMMENT '订单ID',
    order_code VARCHAR(64) COMMENT '订单编号',
    modify_type VARCHAR(32) COMMENT '修改类型',
    old_value VARCHAR(512) COMMENT '原值',
    new_value VARCHAR(512) COMMENT '新值',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted INT DEFAULT 0
);
