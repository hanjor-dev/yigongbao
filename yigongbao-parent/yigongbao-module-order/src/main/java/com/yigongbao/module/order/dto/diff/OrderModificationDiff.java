package com.yigongbao.module.order.dto.diff;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单修改差异（主差异容器）
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class OrderModificationDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 基础信息字段差异
     */
    private List<FieldDiff> infoFields;

    /**
     * 订单项差异
     */
    private ItemsDiff items;

    /**
     * 影像数据差异
     */
    private ImageDiff imageData;

    /**
     * 影像报告差异
     */
    private ImageDiff imageReport;

    /** 免费业务审批文件差异 */
    private ImageDiff approvalFile;
}
