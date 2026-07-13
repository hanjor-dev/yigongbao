package com.yigongbao.module.production.qc.dto;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 批量更新产品UDI码 DTO
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Data
public class BatchUpdateUdiDTO {
    /** 流转卡ID */
    @NotNull(message = "流转卡ID不能为空")
    private Long recordId;

    /** 产品UDI列表 */
    @NotEmpty(message = "产品UDI列表不能为空")
    @Valid
    private List<ProductUdiItem> products;

    /**
     * 产品UDI项
     */
    @Data
    public static class ProductUdiItem {
        /** 产品ID */
        @NotNull(message = "产品ID不能为空")
        private Long productId;

        /** UDI码 */
        @NotBlank(message = "UDI码不能为空")
        private String udiCode;
    }
}
