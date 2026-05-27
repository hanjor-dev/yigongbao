package com.yigongbao.module.production.record.vo;

import com.yigongbao.module.production.product.vo.ProductionProductVO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产流转卡 VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionRecordVO {
    private Long id;
    private String recordNo;
    private Integer orderType;
    private String designPackageCode;
    private String productionBatchNo;
    private Integer totalProductCount;
    private Integer qualifiedCount;
    private Integer unqualifiedCount;
    private Integer hasRedoProduct;
    private String status;
    private String currentProcess;
    private String qrCodeUrl;
    private LocalDateTime createTime;
    private List<ProductionProductVO> products;
}
