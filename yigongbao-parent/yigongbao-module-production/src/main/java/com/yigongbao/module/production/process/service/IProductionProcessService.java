package com.yigongbao.module.production.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.vo.ProcessVO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序操作服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionProcessService extends IService<ProductionProcessEntity> {

    List<ProcessVO> listProcesses(Long recordId);

    void startProcess(Long recordId, StartProcessDTO dto);

    void finishProcess(Long recordId, String processType);

    /**
     * 根据打印完成时间排程固定的后处理工序时间。
     *
     * @param recordId 流转卡ID
     * @param printFinishTime 打印完成时间
     */
    void schedulePostProcessing(Long recordId, LocalDateTime printFinishTime);
}
