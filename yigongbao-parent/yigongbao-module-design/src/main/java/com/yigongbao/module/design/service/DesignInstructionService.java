package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignInstructionEntity;

import java.util.List;

/**
 * 设计指令单服务接口
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignInstructionService extends IService<DesignInstructionEntity> {

    /**
     * 查询数据包最新版本记录（version_seq 最大），无记录时返回 null
     *
     * @param packageId 数据包ID
     * @return 最新版本记录，或 null
     */
    DesignInstructionEntity getLatestVersion(Long packageId);

    /**
     * 查询数据包的版本历史列表（按 version_seq 倒序）
     *
     * @param packageId 数据包ID
     * @return 版本历史列表
     */
    List<DesignInstructionEntity> listVersions(Long packageId);
}
