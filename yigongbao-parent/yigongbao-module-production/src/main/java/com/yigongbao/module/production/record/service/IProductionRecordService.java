package com.yigongbao.module.production.record.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.production.record.dto.CreateRecordDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;

/**
 * 生产流转卡服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionRecordService extends IService<ProductionRecordEntity> {

    Long createRecord(CreateRecordDTO dto);

    ProductionRecordVO getRecordDetail(Long id);

    ProductionRecordVO getByRecordNo(String recordNo);

    String getQrCodeUrl(Long id);

    IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto);

    void downloadDataPackage(Long designPackageId);

    /**
     * 聚合触发：同订单所有活跃流转卡均达到 requiredStatus 时触发 Flow 并回写 order_main
     */
    void triggerFlowIfAllReach(Long orderId, String requiredStatus, FlowActionEnum action);
}
