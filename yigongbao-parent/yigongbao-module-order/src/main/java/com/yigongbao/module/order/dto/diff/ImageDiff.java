package com.yigongbao.module.order.dto.diff;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 影像文件差异
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class ImageDiff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新增的文件ID
     */
    private List<String> added;

    /**
     * 删除的文件ID
     */
    private List<String> deleted;

    /**
     * 判断是否有变更
     */
    public boolean isChanged() {
        return CollUtil.isNotEmpty(added) || CollUtil.isNotEmpty(deleted);
    }
}
