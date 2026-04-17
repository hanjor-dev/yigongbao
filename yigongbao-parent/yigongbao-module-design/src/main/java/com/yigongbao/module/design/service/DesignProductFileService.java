package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignProductFileEntity;

import java.util.List;
import java.util.Set;

/**
 * 打印产品关联文件 Service
 *
 * @author hanjor
 * @date 2026-04-17
 */
public interface DesignProductFileService extends IService<DesignProductFileEntity> {

    /**
     * 查询指定产品行的所有关联文件（按 sort_order 升序）
     *
     * @param designProductId design_product.id
     * @return 文件列表
     */
    List<DesignProductFileEntity> listByProductId(Long designProductId);

    /**
     * 批量查询多个产品行的关联文件
     *
     * @param designProductIds 产品行 ID 列表
     * @return 文件列表
     */
    List<DesignProductFileEntity> listByProductIds(List<Long> designProductIds);

    /**
     * 删除指定产品行的所有关联文件（逻辑删除）
     *
     * @param designProductId design_product.id
     */
    void removeByProductId(Long designProductId);

    /**
     * 批量删除多个产品行的关联文件（逻辑删除）
     *
     * @param designProductIds 产品行 ID 列表
     */
    void removeByProductIds(List<Long> designProductIds);

    /**
     * 返回给定数据包 ID 列表中已关联打印信息的 package_file_id 集合
     * <p>
     * 用于数据包文件列表的 hasPrintInfo 标记。
     * </p>
     *
     * @param packageIds 数据包 ID 列表
     * @return 已关联的 package_file_id 集合
     */
    Set<Long> getFilledPackageFileIds(List<Long> packageIds);
}
