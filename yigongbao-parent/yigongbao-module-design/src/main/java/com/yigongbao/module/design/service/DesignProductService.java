package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignProductEntity;

import java.util.List;

/**
 * 打印产品信息服务接口
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignProductService extends IService<DesignProductEntity> {

    /**
     * 统计数据包关联的打印产品数量
     *
     * @param packageId 数据包ID
     * @return 数量
     */
    long countByPackageId(Long packageId);
}
