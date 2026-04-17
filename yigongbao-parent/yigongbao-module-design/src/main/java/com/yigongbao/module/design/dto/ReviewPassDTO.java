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
}
