package com.yigongbao.module.design.dto;

import lombok.Data;

/**
 * 审核通过请求体
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
public class ReviewPassDTO {

    /**
     * 审批意见（选填）
     */
    private String comment;

    /**
     * 订单版本号（乐观锁）
     * 前端加载订单详情时记录，提交审核时传入，防止基于过期数据的错误审核
     */
    private Integer version;
}
