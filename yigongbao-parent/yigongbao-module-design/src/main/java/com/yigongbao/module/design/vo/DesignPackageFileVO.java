package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据包内文件 VO
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DesignPackageFileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 数据包ID
     */
    private Long packageId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件扩展名
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

    /**
     * 是否已填写打印信息（关联 design_product）
     */
    private Boolean hasPrintInfo;

    /**
     * 包内文件独立 OSS 访问地址（可直接用于 3D 模型渲染）
     */
    private String fileUrl;
    private String downloadUrl;
}
