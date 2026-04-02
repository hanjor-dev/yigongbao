package com.yigongbao.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 文件业务类型枚举
 * 定义系统中所有文件上传的业务类型，枚举 code 与字典 dict_value 对应
 * 枚举 dictCode 与字典 dict_code 对应，用于存储到 file_detail.object_type 字段
 *
 * @author hanjor
 * @date 2026-03-27
 */
@Getter
@AllArgsConstructor
public enum FileBizTypeEnum {

    // ==================== 订单草稿文件 ====================
    /**
     * 订单草稿文件
     * 用于订单草稿阶段的文件关联，业务标识（存储到 file_detail.object_id）
     */
    ORDER_DRAFT("order_draft", "order_draft", "订单草稿文件"),

    // ==================== 影像资料（订单创建） ====================
    /**
     * 影像数据
     * 订单提交时上传CT/MRI等患者影像资料，格式：DICOM/ZIP等
     */
    IMAGE_DATA("image_data", "10.1", "影像数据"),

    /**
     * 影像报告
     * 医生出具的影像分析报告，格式：PDF/JPG等
     */
    IMAGE_REPORT("image_report", "10.2", "影像报告"),

    /**
     * 订单其他附件
     * 订单相关其他资料，格式：无限制
     */
    ORDER_ATTACHMENT("order_attachment", "10.3", "订单其他附件"),

    // ==================== 设计文件（工单设计） ====================
    /**
     * 打印文件包
     * 用于3D打印的设计文件包，每个数据包（ZIP）对应一份指令单和一份图纸，格式：ZIP（含STL/3MF/OBJ）
     */
    PRINT_PACKAGE("print_package", "10.4", "打印文件包"),

    /**
     * 设计报告
     * 设计说明、设计参数等，格式：PDF/Word
     */
    DESIGN_REPORT("design_report", "10.5", "设计报告"),

    /**
     * 可视化模型
     * 用于展示的三维渲染图/效果图，用于图纸中展示，格式：3DPDF/PLY/STL
     */
    VISUAL_MODEL("visual_model", "10.6", "可视化模型"),

    /**
     * 图纸文件
     * 系统生成的图纸文件，可上传修订版，格式：PDF
     */
    DRAWING_FILE("drawing_file", "10.7", "图纸文件"),

    /**
     * 指令单文件
     * 系统生成的指令单文件，可上传修订版，格式：PDF
     */
    INSTRUCTION_FILE("instruction_file", "10.8", "指令单文件"),

    // ==================== 电子签名 ====================
    /**
     * 签名图片
     * 设计师/审核人员预设签名图片，格式：PNG/JPG
     */
    SIGNATURE_IMAGE("signature_image", "10.9", "签名图片"),

    // ==================== 医生相关 ====================
    /**
     * 医生资质证明
     * 医生执业证书等，格式：PDF/JPG
     */
    DOCTOR_CERT("doctor_cert", "10.10", "医生资质证明"),

    /**
     * 医生头像
     * 医生个人照片，格式：PNG/JPG
     */
    DOCTOR_AVATAR("doctor_avatar", "10.11", "医生头像"),

    // ==================== 医院/机构相关 ====================
    /**
     * 医院资质文件
     * 营业执照等，格式：PDF/JPG
     */
    HOSPITAL_CERT("hospital_cert", "10.12", "医院资质文件"),

    /**
     * 医院图片
     * Logo等，格式：PNG/JPG
     */
    HOSPITAL_AVATAR("hospital_avatar", "10.13", "医院图片"),

    // ==================== 产品相关 ====================
    /**
     * 产品图片
     * 产品展示图片，格式：PNG/JPG
     */
    PRODUCT_IMAGE("product_image", "10.14", "产品图片"),

    // ==================== 注册证相关 ====================
    /**
     * 注册证扫描件
     * 注册证电子扫描件，格式：PDF/JPG
     */
    REGISTRATION_CERT("registration_cert", "10.15", "注册证扫描件"),

    // ==================== 模板相关 ====================
    /**
     * 模板附件
     * 医院组合模板相关附件
     */
    TEMPLATE_ATTACHMENT("template_attachment", "10.16", "模板附件"),

    // ==================== 通用 ====================
    /**
     * 通用文件
     * 其他未分类附件
     */
    COMMON("common", "10.17", "通用文件");

    /**
     * 枚举 code，字典 dict_value
     * 前端上传时传入此值，系统据此查找枚举
     */
    private final String code;

    /**
     * 字典 dict_code，存储到 file_detail.object_type 字段
     */
    private final String dictCode;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 根据字典 code 查找枚举
     *
     * @param code 字典 code（dict_code），如 "10.1"、"10.4"
     * @return 对应的枚举值，未找到返回 null
     */
    public static FileBizTypeEnum getByDictCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getDictCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据枚举 code 查找枚举
     *
     * @param code 枚举 code，如 "image_data"、"doctor_cert"
     * @return 对应的枚举值，未找到返回 null
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
