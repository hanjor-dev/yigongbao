package com.yigongbao.module.order.dto.order;

import com.yigongbao.module.order.vo.order.OrderColumnConfigVO.ColumnItemVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 保存列配置 DTO
 *
 * @author hanjor
 * @date 2026-04-06
 */
@Data
@Schema(description = "保存列配置请求参数")
public class SaveColumnConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 列配置列表
     */
    @Schema(description = "列配置列表")
    @NotNull(message = "列配置不能为空")
    private List<ColumnItemVO> columns;
}
