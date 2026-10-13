package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单草稿文件关联实体
 *
 * @author hanjor
 * @date 2026-06-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_draft_file")
public class OrderDraftFileEntity extends BaseEntity {

    /**
     * 草稿ID
     */
    private Long draftId;

    /**
     * 文件ID（file_detail.id）
     */
    private String fileId;

    /**
     * 文件类别（字典 dict_code）
     */
    private String fileCategory;

    /**
     * 数据包编号
     */
    private String packageNo;

    /**
     * 关联的草稿明细ID
     */
    private Long orderItemDraftId;
}
