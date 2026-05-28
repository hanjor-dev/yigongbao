package com.yigongbao.module.production.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.dto.SubmitProcessQcDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.vo.ProcessVO;
import java.util.List;

/**
 * 工序操作服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionProcessService extends IService<ProductionProcessEntity> {

    void fillProcess(Long processId, FillProcessDTO dto);

    void submitProcessQc(Long processId, SubmitProcessQcDTO dto);

    Long handlePrintFailure(Long recordId, String failureReason, boolean recreate);

    Long handlePrintInspectionFail(Long recordId, String failureReason, boolean recreate);

    List<ProcessVO> listProcesses(Long recordId);

    void startProcess(Long recordId, StartProcessDTO dto);

    void finishProcess(Long recordId, String processType);
}
