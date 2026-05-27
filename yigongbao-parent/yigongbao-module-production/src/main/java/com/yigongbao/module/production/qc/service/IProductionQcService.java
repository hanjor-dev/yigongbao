package com.yigongbao.module.production.qc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.qc.dto.ProductionRedoPageDTO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;

import java.util.List;

public interface IProductionQcService {
    void markProductPass(Long productId);
    void markProductRedo(Long productId, String reason, String handleType);
    void assignRedoProcess(Long productId, String processType);
    void transferToPacking(Long recordId);
    List<ProductionProductVO> listProductsByRecordId(Long recordId);
    IPage<ProductionRecordVO> listQcRecords(ProductionQcPageDTO dto);
    IPage<ProductionProductVO> listRedoProducts(ProductionRedoPageDTO dto);
}
