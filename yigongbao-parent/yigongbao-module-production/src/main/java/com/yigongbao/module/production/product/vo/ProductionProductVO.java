package com.yigongbao.module.production.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 生产产品 VO（简要信息，用于流转卡详情和质检列表）
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionProductVO {
    private Long id;
    /** 产品编号 */
    private String productNo;
    /** 产品名称 */
    private String productName;
    /** 型号规格 */
    private String specName;
    /** 注册证号 */
    private String certNo;
    /** 材质名称 */
    private String materialName;
    /** 颜色名称 */
    private String colorName;
    /** 打印文件名 */
    private String fileName;
    /** 产品重量，单位：克 */
    private BigDecimal weight;
    /** 产品状态代码（in_process/fail/pass/completed/cancelled） */
    private String status;
    /** 产品状态中文名 */
    private String statusName;
    /** 质检结果（pass/fail） */
    private String qcResult;
    /** UDI码（医疗器械质检合格后生成） */
    private String udiCode;
}
