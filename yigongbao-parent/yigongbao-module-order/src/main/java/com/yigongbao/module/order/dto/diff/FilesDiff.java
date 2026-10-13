package com.yigongbao.module.order.dto.diff;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件差异
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class FilesDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新增影像资料数量
     */
    private Integer imageDataAdded;

    /**
     * 删除影像资料数量
     */
    private Integer imageDataDeleted;

    /**
     * 新增影像报告数量
     */
    private Integer imageReportAdded;

    /**
     * 删除影像报告数量
     */
    private Integer imageReportDeleted;
}
