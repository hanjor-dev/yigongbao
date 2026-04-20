package com.yigongbao.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 文件业务类型枚举
 * 定义系统中所有文件上传的业务类型
 * <p>
 * 字段说明：
 * - code：枚举标识，存储路径前缀，前端上传时传入 bizType 参数（即 dict_code）
 * - dictCode：字典编码（dict_code），存储到 file_detail.object_type 字段，前端用此值作为 bizType
 * - name：中文名称
 * - configPrefix：对应 sys_config 中的配置前缀（null 表示无格式/大小限制）
 *   约定：configPrefix + ".allowed_extensions" = 允许扩展名配置键
 *         configPrefix + ".max_size_mb"        = 最大大小（MB）配置键
 *   sys_dict.dict_value 存储此值，可通过字典管理界面新增业务类型时指定
 *
 * @author hanjor
 * @date 2026-03-27
 */
@Getter
@AllArgsConstructor
public enum FileBizTypeEnum {

    // ==================== 订单草稿文件 ====================
    /**
     * 订单草稿文件（无格式限制）
     */
    ORDER_DRAFT("order_draft", "order_draft", "订单草稿文件", null),

    // ==================== 影像资料（订单创建） ====================
    /**
     * 影像数据：CT/MRI 等患者影像资料，格式 ZIP/RAR/7Z
     */
    IMAGE_DATA("image_data", "10.1", "影像数据", "order.image.data"),

    /**
     * 影像报告：医生出具的影像分析报告，格式 PDF/Word/Excel
     */
    IMAGE_REPORT("image_report", "10.2", "影像报告", "order.image.report"),

    /**
     * 订单其他附件（无格式限制）
     */
    ORDER_ATTACHMENT("order_attachment", "10.3", "订单其他附件", null),

    // ==================== 设计文件（工单设计） ====================
    /**
     * 打印文件包：3D 打印设计文件压缩包，格式 ZIP/RAR/7Z（内含 STL/3MF/OBJ）
     * configPrefix=null：容器格式和大小由 DesignFileServiceImpl.uploadPackage 手动校验，
     * 不走 FileUploadConfigProvider，避免与内容文件格式（design.package.allowed_extensions）混淆。
     */
    PRINT_PACKAGE("print_package", "10.4", "打印文件包", null),

    /**
     * 设计报告：设计说明、设计参数等，格式 PDF/Word/Excel
     */
    DESIGN_REPORT("design_report", "10.5", "设计报告", "design.report"),

    /**
     * 可视化模型：三维渲染/效果图，格式 STL/OBJ/PLY/3MF
     */
    VISUAL_MODEL("visual_model", "10.6", "可视化模型", "design.model"),

    /**
     * 图纸文件：系统生成或上传的修订版图纸（无格式限制，服务端自行校验）
     */
    DRAWING_FILE("drawing_file", "10.7", "图纸文件", null),

    /**
     * 图纸截图：设计师在 viewer 中标注后上传的截图，格式 PNG/JPG
     */
    IMAGE_SCREENSHOT("image_screenshot", "10.18", "图纸截图", null),

    /**
     * 指令单文件：系统生成或上传的修订版指令单（无格式限制）
     */
    INSTRUCTION_FILE("instruction_file", "10.8", "指令单文件", null),

    // ==================== 电子签名 ====================
    /**
     * 签名图片（无格式限制）
     */
    SIGNATURE_IMAGE("signature_image", "10.9", "签名图片", null),

    // ==================== 医生相关 ====================
    /**
     * 医生资质证明（无格式限制）
     */
    DOCTOR_CERT("doctor_cert", "10.10", "医生资质证明", null),

    /**
     * 医生头像（无格式限制）
     */
    DOCTOR_AVATAR("doctor_avatar", "10.11", "医生头像", null),

    // ==================== 医院/机构相关 ====================
    /**
     * 医院资质文件（无格式限制）
     */
    HOSPITAL_CERT("hospital_cert", "10.12", "医院资质文件", null),

    /**
     * 医院图片（无格式限制）
     */
    HOSPITAL_AVATAR("hospital_avatar", "10.13", "医院图片", null),

    // ==================== 产品相关 ====================
    /**
     * 产品图片（无格式限制）
     */
    PRODUCT_IMAGE("product_image", "10.14", "产品图片", null),

    // ==================== 注册证相关 ====================
    /**
     * 注册证扫描件（无格式限制）
     */
    REGISTRATION_CERT("registration_cert", "10.15", "注册证扫描件", null),

    // ==================== 模板相关 ====================
    /**
     * 模板附件（无格式限制）
     */
    TEMPLATE_ATTACHMENT("template_attachment", "10.16", "模板附件", null),

    // ==================== 通用 ====================
    /**
     * 通用文件（无格式限制）
     */
    COMMON("common", "10.17", "通用文件", null);

    /**
     * 枚举标识，用于存储路径前缀（如 "image_data"）
     */
    private final String code;

    /**
     * 字典编码（dict_code），前端上传时传入 bizType 参数的值，存储到 file_detail.object_type
     */
    private final String dictCode;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * sys_config 配置前缀（null 表示该类型无格式/大小限制，直接放行）
     * 约定规则：
     *   allowedExtensions key = configPrefix + ".allowed_extensions"
     *   maxSizeMb         key = configPrefix + ".max_size_mb"
     * 与 sys_dict.dict_value 保持一致，便于管理界面新增类型时填写
     */
    private final String configPrefix;

    /**
     * 根据 dict_code 查找枚举（前端传入 bizType 时使用）
     *
     * @param dictCode dict_code，如 "10.1"
     * @return 对应枚举，未找到返回 null
     */
    public static FileBizTypeEnum getByDictCode(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getDictCode().equals(dictCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据枚举 code 查找枚举
     *
     * @param code 枚举 code，如 "image_data"
     * @return 对应枚举，未找到返回 null
     */
    public static FileBizTypeEnum getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
