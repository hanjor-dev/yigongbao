package com.yigongbao.module.production.record.service;

import com.yigongbao.module.production.record.dto.ForceCompletePrintDTO;

/** 打印生命周期补偿服务。 */
public interface ProductionPrintLifecycleService {

    void forceCompletePrint(Long recordId, ForceCompletePrintDTO dto);

    /**
     * 由设备事件或人工补偿共用的打印完成逻辑。
     *
     * @return 是否实际完成了状态更新
     */
    boolean completePrint(Long recordId, String source);
}
