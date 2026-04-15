package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存打印信息单条记录 DTO
 * certNo 字段由后端从 product_spec.cert_no 自动覆盖，前端传值被忽略
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class SavePrintInfoItemDTO {

    @NotNull(message = "数据包文件ID不能为空")
    private Long packageFileId;

    /** 文件名（冗余） */
    private String packageFileName;

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /** 产品名称（冗余） */
    private String productName;

    @NotNull(message = "规格ID不能为空")
    private Long specId;

    /** 规格名称（冗余） */
    private String specName;

    /** 注册证号（冗余），后端从 product_spec.cert_no 自动覆盖，前端传值被忽略 */
    private String certNo;

    /** 材质 dict_code（如 15.1） */
    private String materialId;

    /** 材质名称（冗余） */
    private String materialName;

    /** 颜色 dict_code（如 16.1.1） */
    private String colorId;

    /** 颜色名称（冗余） */
    private String colorName;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    private Integer quantity;

    private Integer packQuantity;

    private String timeliness;

    private String productMark;

    private Integer sortOrder;
}
