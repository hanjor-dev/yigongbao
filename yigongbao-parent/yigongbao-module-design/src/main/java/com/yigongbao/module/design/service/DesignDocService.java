package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DocItemVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 指令单/图纸生成与管理服务接口
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface DesignDocService {

    /**
     * 生成指令单（填充 Excel 模板 → 上传 → 保存记录）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果（id、version、fileId、url）
     */
    DocItemVO generateInstruction(Long orderId, Long packageId);

    /**
     * 生成图纸（填充 Excel 模板 → 上传 → 保存记录）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果（id、version、fileId、url）
     */
    DocItemVO generateDrawing(Long orderId, Long packageId);

    /**
     * 查询指令单版本历史列表
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 版本列表（按 versionSeq 倒序）
     */
    List<DesignDocVersionVO> listInstructionVersions(Long orderId, Long packageId);

    /**
     * 查询图纸版本历史列表
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 版本列表（按 versionSeq 倒序）
     */
    List<DesignDocVersionVO> listDrawingVersions(Long orderId, Long packageId);

    /**
     * 上传修订版指令单
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID（要更新的版本）
     * @param file      修订版文件
     */
    void uploadRevisedInstruction(Long orderId, Long packageId, Long id, MultipartFile file);

    /**
     * 上传修订版图纸
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID（要更新的版本）
     * @param file      修订版文件
     */
    void uploadRevisedDrawing(Long orderId, Long packageId, Long id, MultipartFile file);

    /**
     * 确认图纸（在线模式专用）
     * <p>
     * 在线模式下，设计师预览生成的图纸满意后调用此接口，将 is_confirmed 置为 1。
     * 离线模式下无需调用（上传修订版时自动确认）。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID
     */
    void confirmDrawing(Long orderId, Long packageId, Long id);

    /**
     * 确认指令单（在线模式专用）
     * <p>
     * 在线模式下，设计师确认生成的指令单内容无误后调用此接口，将 is_confirmed 置为 1。
     * 离线模式下无需调用（上传修订版时自动确认）。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID
     */
    void confirmInstruction(Long orderId, Long packageId, Long id);
}
