package com.yigongbao.module.production.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;

/**
 * 工序操作服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionProcessService extends IService<ProductionProcessEntity> {

    void fillProcess(Long processId, FillProcessDTO dto);

    void transferToNext(Long recordId, String fromProcess, String toProcess);

    Long handlePrintFailure(Long recordId, String failureReason, boolean recreate);

    Long handlePrintInspectionFail(Long recordId, String failureReason, boolean recreate);
}
