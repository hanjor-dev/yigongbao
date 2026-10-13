package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 完成设计DTO
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Data
public class CompleteDesignDTO {

    /**
     * 订单版本号（乐观锁）
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
