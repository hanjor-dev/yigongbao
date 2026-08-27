package com.yigongbao.module.production.record.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.ProductLedgerExportDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SaveProductionColumnConfigDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.vo.*;
import com.yigongbao.module.basic.file.vo.FileVO;
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

    String downloadDataPackage(Long recordId);

    String generateBatchNo(Long recordId);

    void submitBatchNo(Long recordId, SubmitBatchNoDTO dto);

    DeviceConfigVO getDeviceConfig(Long recordId);

    List<ProcessingCenterPrintersVO> listPrinters();

    PrinterOccupationVO getPrinterOccupation(Long recordId, Long deviceId);

    void assignDevice(Long recordId, AssignDeviceDTO dto);

    void releaseDevice(Long recordId);

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

    /**
     * 根据订单下有效流转卡的最小主线进度，补偿推进订单状态。
     * 仅通过 Flow 状态机推进父订单，不直接改写 order_main 状态。
     */
    void reconcileOrderProductionStatus(Long orderId);

    /**
     * 获取流转卡取消预查询信息
     */
    CancelPreviewVO getCancelPreview(Long recordId);

    /**
     * 生成流转卡Excel文件（强制生成，不使用缓存）
     * @param recordId 流转卡ID
     * @return 文件信息
     */
    FileVO generateFlowCardExcel(Long recordId);

    /**
     * 获取当前用户的列配置
     */
    ProductionColumnConfigVO getColumnConfig();

    /**
     * 保存当前用户的列配置
     */
    void saveColumnConfig(SaveProductionColumnConfigDTO dto);

    /**
     * 导出生产产品台账Excel
     * <p>
     * 功能说明：
     * 1. 数据粒度：产品级别（非订单级别、非流转卡级别）
     * 2. 导出字段：41个字段，涵盖产品信息、订单信息、流转卡信息、工序人员、质检仓储
     * 3. 数据权限：根据当前用户角色自动过滤（医院/加工中心/全部）
     * 4. 导出限制：最多1万条，超出时Excel顶部显示红色警告
     * 5. 参数校验：至少指定一个查询条件，防止无条件全表导出
     * </p>
     *
     * @param dto 查询条件（recordNo/orderCode/productNo/startTime/endTime）
     * @return Excel文件字节数组
     * @throws BusinessException 无查询条件、无权限、查询结果为空、Excel生成失败时抛出
     */
    byte[] exportProductLedger(ProductLedgerExportDTO dto);
}
