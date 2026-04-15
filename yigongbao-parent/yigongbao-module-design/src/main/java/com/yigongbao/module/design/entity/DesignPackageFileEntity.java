package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据包内文件 Entity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_package_file")
@EqualsAndHashCode(callSuper = false)
public class DesignPackageFileEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 数据包ID
     */
    private Long packageId;

    /**
     * 文件名（如 左髋骨.stl）
     */
    private String fileName;

    /**
     * 文件扩展名（stl/3mf/obj）
     */
    private String fileExt;

    /**
     * 包内相对路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
