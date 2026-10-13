package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动分配设计师 DTO
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Data
public class AssignDesignerDTO {

    /**
     * 设计师用户ID
     */
    @NotNull(message = "设计师ID不能为空")
    private Long designerId;
}
