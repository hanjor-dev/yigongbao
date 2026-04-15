package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignProductEntity;

import java.util.List;
import java.util.Set;

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

    /**
     * 获取已填写打印信息的包内文件ID集合
     *
     * @param packageIds 数据包ID列表
     * @return 文件ID集合
     */
    Set<Long> getFilledFileIds(List<Long> packageIds);
}
