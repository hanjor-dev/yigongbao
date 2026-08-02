package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignDrawingEntity;

import java.util.List;

/**
 * 设计图纸服务接口
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignDrawingService extends IService<DesignDrawingEntity> {

    /**
     * 查询数据包最新版本记录（version_seq 最大），无记录时返回 null
     *
     * @param packageId 数据包ID
     * @return 最新版本记录，或 null
     */
    DesignDrawingEntity getLatestVersion(Long packageId);

    DesignDrawingEntity getLatestVersion(Long packageId, String productCategory);

    /**
     * 查询数据包的版本历史列表（按 version_seq 倒序）
     *
     * @param packageId 数据包ID
     * @return 版本历史列表
     */
    List<DesignDrawingEntity> listVersions(Long packageId);

    List<DesignDrawingEntity> listVersions(Long packageId, String productCategory);
}
