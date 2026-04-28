package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 系统配置键枚举
 * 集中管理系统配置键名，避免魔法值和键名不一致问题
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Getter
@AllArgsConstructor
public enum SystemConfigKeyEnum {

    // ==================== 安全配置 ====================
    /**
     * 默认密码
     * 新用户初始密码
     */
    DEFAULT_PASSWORD("default.password", "默认密码"),

    /**
     * 最大连续登录失败次数
     * 连续失败后锁定账号
     */
    LOGIN_MAX_FAILURES("login.max.failures", "最大连续登录失败次数"),

    /**
     * 登录锁定时长（分钟）
     * 自动解锁时间
     */
    LOGIN_LOCK_DURATION("login.lock.duration", "登录锁定时长"),

    /**
     * 验证码有效期（秒）
     */
    CAPTCHA_EXPIRE_SECONDS("captcha.expire.seconds", "验证码有效期（秒）"),

    /**
     * 同一目标发送冷却（秒）
     */
    CAPTCHA_COOLDOWN_SECONDS("captcha.cooldown.seconds", "验证码发送冷却时间（秒）"),

    /**
     * 同一目标每日最大发送次数
     */
    CAPTCHA_DAILY_LIMIT("captcha.daily.limit", "验证码每日最大发送次数"),

    /**
     * 发件人邮箱地址
     */
    MAIL_FROM("mail.from", "发件人邮箱地址"),

    // ==================== 系统配置 ====================
    /**
     * 系统名称
     */
    SYSTEM_NAME("system.name", "系统名称"),

    /**
     * 文件上传最大大小（字节）
     */
    MAX_UPLOAD_SIZE("max.upload.size", "文件上传最大大小"),

    // ==================== 订单配置 ====================
    /**
     * 提交订单是否必须上传影像文件
     * true - 必须上传（默认）
     * false - 非必填
     */
    ORDER_IMAGE_REQUIRED("order.image.required", "提交订单是否必须上传影像文件"),

    /**
     * 影像数据包允许的文件扩展名（逗号分隔）
     * 默认：.zip,.rar,.7z
     */
    ORDER_IMAGE_DATA_ALLOWED_EXTENSIONS("order.image.data.allowed_extensions", "影像数据包允许的文件扩展名"),

    /**
     * 影像数据包最大文件大小（MB）
     * 默认：500 MB；由 FileUploadConfigProvider 通过 configPrefix 读取，无需业务代码直接调用
     */
    ORDER_IMAGE_DATA_MAX_SIZE_MB("order.image.data.max_size_mb", "影像数据包最大文件大小（MB）"),

    /**
     * 影像报告允许的文件扩展名（逗号分隔）
     * 默认：.pdf,.doc,.docx,.xls,.xlsx
     */
    ORDER_IMAGE_REPORT_ALLOWED_EXTENSIONS("order.image.report.allowed_extensions", "影像报告允许的文件扩展名"),

    /**
     * 影像报告最大文件大小（MB）
     * 默认：50 MB；由 FileUploadConfigProvider 通过 configPrefix 读取
     */
    ORDER_IMAGE_REPORT_MAX_SIZE_MB("order.image.report.max_size_mb", "影像报告最大文件大小（MB）"),

    /**
     * 草稿自动过期天数
     * 默认 30 天
     */
    ORDER_DRAFT_EXPIRE_DAYS("order.draft.expire.days", "草稿自动过期天数"),

    /**
     * 订单提交后修改窗口期（分钟）
     * 默认 10 分钟
     */
    ORDER_MODIFY_WINDOW_MINUTES("order.modify.window.minutes", "订单提交后修改窗口期（分钟）"),

    /**
     * 订单列表默认列配置（JSON 格式）
     */
    ORDER_COLUMN_CONFIG("order.column.config", "订单列表默认列配置"),

    /**
     * 订单修改申请字段配置（JSON 格式，顶层 key 为申请类型字典编码）
     * 例：{"14.1":{"name":"基础信息","fields":[...]}}
     */
    ORDER_MODIFY_FIELD_CONFIG("order.modify.field.config", "订单修改申请字段配置"),

    // ==================== 流程状态机配置 ====================
    /**
     * 最大允许的审核驳回次数
     */
    FLOW_MAX_AUDIT_REJECT("flow.max.audit.reject", "最大允许的审核驳回次数"),

    /**
     * 最大允许的返工次数
     */
    FLOW_MAX_REWORK("flow.max.rework", "最大允许的返工次数"),

    /**
     * 最大允许的设计审核驳回次数
     */
    FLOW_MAX_DESIGN_REJECT("flow.max.design.reject", "最大允许的设计审核驳回次数"),

    // ==================== 设计师分配配置 ====================
    /**
     * 设计师分配模式（auto-自动分配，manual-手动分配）
     */
    DESIGN_ASSIGN_MODE("design.assign.mode", "设计师分配模式"),

    // ==================== 设计文件配置 ====================
    /**
     * 设计文件数据包容器格式（压缩包本身允许的扩展名，逗号分隔）
     * 默认：.zip,.rar,.7z
     */
    DESIGN_PACKAGE_ARCHIVE_EXTENSIONS("design.package.archive_extensions", "设计文件数据包容器格式"),

    /**
     * 设计文件数据包最大文件大小（MB）
     * 默认：500 MB；由 FileUploadConfigProvider 通过 configPrefix 读取
     */
    DESIGN_PACKAGE_MAX_SIZE_MB("design.package.max_size_mb", "设计文件数据包最大文件大小（MB）"),

    /**
     * 数据包内部允许的文件扩展名（逗号分隔，用于解析压缩包内容）
     */
    DESIGN_PACKAGE_ALLOWED_EXTENSIONS("design.package.allowed_extensions", "数据包内部允许的文件扩展名"),

    /**
     * 设计报告允许的文件扩展名（逗号分隔）
     * 默认：.pdf,.doc,.docx,.xls,.xlsx
     */
    DESIGN_REPORT_ALLOWED_EXTENSIONS("design.report.allowed_extensions", "设计报告允许的文件扩展名"),

    /**
     * 设计报告最大文件大小（MB）
     * 默认：50 MB；由 FileUploadConfigProvider 通过 configPrefix 读取
     */
    DESIGN_REPORT_MAX_SIZE_MB("design.report.max_size_mb", "设计报告最大文件大小（MB）"),

    /**
     * 可视化模型允许的文件扩展名（逗号分隔）
     * 默认：.stl,.obj,.ply,.3mf
     */
    DESIGN_MODEL_ALLOWED_EXTENSIONS("design.model.allowed_extensions", "可视化模型允许的文件扩展名"),

    /**
     * 可视化模型最大文件大小（MB）
     * 默认：200 MB；由 FileUploadConfigProvider 通过 configPrefix 读取
     */
    DESIGN_MODEL_MAX_SIZE_MB("design.model.max_size_mb", "可视化模型最大文件大小（MB）"),

    /**
     * 设计模式（1=线下修改，2=在线编辑）
     */
    DESIGN_MODE("design.mode", "设计模式"),

    // ==================== 设计工单列配置 ====================
    /**
     * 设计工单列表默认列配置（JSON 格式）
     */
    DESIGN_COLUMN_CONFIG("design.column.config", "设计工单列表默认列配置"),

    // ==================== 机构配置 ====================
    /**
     * 机构资质文件允许的文件扩展名（逗号分隔）
     * 默认：zip,rar,tar,7z
     */
    ORG_CERT_ALLOWED_EXTENSIONS("org.cert.allowed_extensions", "资质文件允许格式"),

    /**
     * 机构资质文件最大大小（MB）
     * 默认：500 MB
     */
    ORG_CERT_MAX_SIZE_MB("org.cert.max_size_mb", "资质文件最大大小(MB)"),

    /**
     * 生产企业机构ID（系统预设唯一生产企业）
     */
    MANUFACTURER_ORG_ID("manufacturer.org.id", "生产企业机构ID");

    /**
     * 配置键
     */
    private final String key;

    /**
     * 配置名称（用于日志和错误提示）
     */
    private final String name;

    /**
     * 配置值类型
     * 用于反射获取值后的类型转换
     */
    @Getter
    public enum ConfigValueType {
        STRING,
        INTEGER,
        LONG,
        BOOLEAN
    }

    /**
     * 根据配置键查找枚举
     *
     * @param key 配置键，如 "default.password"
     * @return 对应的枚举值，未找到返回 null
     */
    public static SystemConfigKeyEnum getByKey(String key) {
        if (key == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }
}
