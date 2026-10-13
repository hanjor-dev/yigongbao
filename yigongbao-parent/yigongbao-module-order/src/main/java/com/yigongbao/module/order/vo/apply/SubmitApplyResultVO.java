package com.yigongbao.module.order.vo.apply;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交修改申请结果VO
 *
 * @author hanjor
 * @since 2026-06-08
 */
@Data
public class SubmitApplyResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    private Long applyId;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
