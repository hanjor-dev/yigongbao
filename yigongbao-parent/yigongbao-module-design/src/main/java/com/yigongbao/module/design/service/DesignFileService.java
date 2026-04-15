package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.basic.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 设计文件服务接口
 * 数据包：直接上传（需解析压缩包）
 * 模型/报告：关联已上传的文件（通过 FileController 上传）
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignFileService {

    // ==================== 数据包 ====================

    /**
     * 上传打印文件数据包
     *
     * @param orderId 订单ID
     * @param file    压缩包文件（支持 ZIP/RAR/7Z）
     * @return 数据包信息
     */
    DesignPackageVO uploadPackage(Long orderId, MultipartFile file);

    /**
     * 删除数据包
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     */
    void deletePackage(Long orderId, Long packageId);

    /**
     * 获取订单的数据包列表
     *
     * @param orderId 订单ID
     * @return 数据包列表
     */
    List<DesignPackageVO> listPackages(Long orderId);

    // ==================== 可视化模型 ====================

    /**
     * 批量关联可视化模型文件
     * 文件需先通过 FileController 上传
     *
     * @param orderId 订单ID
     * @param fileIds 文件ID列表
     * @return 模型信息列表
     */
    List<DesignModelVO> linkModels(Long orderId, List<String> fileIds);

    /**
     * 删除可视化模型
     *
     * @param orderId 订单ID
     * @param modelId 模型ID
     */
    void deleteModel(Long orderId, Long modelId);

    /**
     * 获取订单的可视化模型列表
     *
     * @param orderId 订单ID
     * @return 模型列表
     */
    List<DesignModelVO> listModels(Long orderId);

    // ==================== 设计报告 ====================

    /**
     * 关联设计报告
     * 文件需先通过 FileController 上传
     *
     * @param orderId 订单ID
     * @param fileId  文件ID
     * @return 文件信息
     */
    FileVO linkReport(Long orderId, String fileId);

    /**
     * 删除设计报告
     *
     * @param orderId 订单ID
     * @param fileId  文件ID
     */
    void deleteReport(Long orderId, String fileId);

    /**
     * 获取订单的设计报告
     *
     * @param orderId 订单ID
     * @return 文件信息，无报告返回 null
     */
    FileVO getReport(Long orderId);
}
