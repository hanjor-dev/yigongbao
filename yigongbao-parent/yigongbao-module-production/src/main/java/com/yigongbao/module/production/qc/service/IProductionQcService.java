package com.yigongbao.module.production.qc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.BatchUpdateUdiDTO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;

import java.util.List;

public interface IProductionQcService {
    void markProductPass(Long productId);
    void markProductFail(Long productId, String reason);
    void transferToPacking(Long recordId);
    List<ProductionProductVO> listProductsByRecordId(Long recordId);
    IPage<ProductionRecordVO> listQcRecords(ProductionQcPageDTO dto);

    /**
     * 批量更新产品UDI码
     *
     * @param dto 批量更新请求
     * @throws BusinessException 流转卡状态不允许、UDI码重复、非医疗器械等
     */
    void batchUpdateUdi(BatchUpdateUdiDTO dto);
}
