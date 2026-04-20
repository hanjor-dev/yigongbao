package com.yigongbao.module.imaging.service;

import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;

import java.util.List;

/**
 * 影像阅览服务接口
 *
 * @author hanjor
 * @date 2026-04-20
 */
public interface ImagingService {

    /**
     * 获取订单的DCM影像数据包列表
     *
     * @param orderId 订单ID
     * @return DCM影像包列表
     */
    List<DcmPackageVO> getDcmPackages(Long orderId);

    /**
     * 获取指定数据包内的模型文件列表（含颜色透明度）
     *
     * @param packageId 数据包ID
     * @return 模型文件列表
     */
    List<PackageModelFileVO> getPackageModelFiles(Long packageId);

    /**
     * 获取订单所有数据包内的模型文件，按包分组（含颜色透明度）
     *
     * @param orderId 订单ID
     * @return 按包分组的模型文件列表
     */
    List<PackageModelGroupVO> getPackageModelFilesByOrder(Long orderId);

    /**
     * 获取订单的可视化模型列表（含颜色透明度）
     *
     * @param orderId 订单ID
     * @return 可视化模型列表
     */
    List<ModelVO> getModels(Long orderId);
}
