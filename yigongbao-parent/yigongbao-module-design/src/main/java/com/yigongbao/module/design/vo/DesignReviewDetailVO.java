package com.yigongbao.module.design.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 设计审核详情 VO
 * 在工单详情基础上追加审核历史列表
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DesignReviewDetailVO extends DesignWorkorderDetailVO {

    /**
     * 审核历史记录列表（时间倒序）
     */
    private List<DesignReviewHistoryVO> reviewHistory;
}
