package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 指令单/图纸生成与管理服务接口
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface DesignDocService {

    /**
     * 同时生成指令单和图纸（填充 Excel 模板 → 上传 → 保存记录）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果（两个文档的 id、version、url）
     */
    GenerateDocsResultVO generateDocs(Long orderId, Long packageId);

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
     * 下载指定版本的指令单（模板版）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID
     * @param response  HTTP 响应
     * @throws IOException IO异常
     */
    void downloadInstruction(Long orderId, Long packageId, Long id, HttpServletResponse response) throws IOException;

    /**
     * 下载指定版本的图纸（模板版）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID
     * @param response  HTTP 响应
     * @throws IOException IO异常
     */
    void downloadDrawing(Long orderId, Long packageId, Long id, HttpServletResponse response) throws IOException;

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
}
