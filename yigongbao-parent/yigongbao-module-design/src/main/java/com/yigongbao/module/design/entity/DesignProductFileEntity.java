package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印产品关联文件 Entity
 * 存储产品行与数据包内文件的一对多关联
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("design_product_file")
public class DesignProductFileEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关联 design_product.id
     */
    private Long designProductId;

    /**
     * 关联 design_package_file.id
     */
    private Long packageFileId;

    /**
     * 文件名（冗余）
     */
    private String packageFileName;

    /**
     * 排序
     */
    private Integer sortOrder;
}
