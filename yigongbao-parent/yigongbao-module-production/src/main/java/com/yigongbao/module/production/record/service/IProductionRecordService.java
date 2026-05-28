package com.yigongbao.module.production.record.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.vo.DeviceConfigVO;
import com.yigongbao.module.production.record.vo.PrinterVO;
import com.yigongbao.module.production.record.vo.ProcessingCenterPrintersVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import java.util.List;

/**
 * 生产流转卡服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionRecordService extends IService<ProductionRecordEntity> {

    ProductionRecordVO getRecordDetail(Long id);

    ProductionRecordVO getByRecordNo(String recordNo);

    String getQrCodeUrl(Long id);

    IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto);

    void downloadDataPackage(Long designPackageId);

    String generateBatchNo(Long recordId);

    void submitBatchNo(Long recordId, SubmitBatchNoDTO dto);

    DeviceConfigVO getDeviceConfig(Long recordId);

    List<ProcessingCenterPrintersVO> listPrinters();

    void assignDevice(Long recordId, AssignDeviceDTO dto);

    /**
     * 聚合触发：同订单所有活跃流转卡均达到 requiredStatus 时触发 Flow 并回写 order_main
     */
    void triggerFlowIfAllReach(Long orderId, Integer requiredStatus, FlowActionEnum action);

    /**
     * 聚合触发（精确匹配）：同订单所有活跃流转卡状态均精确等于 exactStatus 时触发 Flow
     * 用于回退场景（REWORK_TO_PRINT），不能用 ≥ 判断
     */
    void triggerFlowIfAllExact(Long orderId, Integer exactStatus, FlowActionEnum action);

    /**
     * 直接触发 Flow 状态流转并回写 order_main（无聚合条件）
     */
    void triggerFlowAndSync(Long orderId, FlowActionEnum action);
}
