package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DocItemVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 指令单/图纸管理服务接口
 * <p>
 * 生成逻辑已内化为"按需自动生成"：
 * - 线下模式：调用 downloadInstruction/downloadDrawing，后端检测数据变化后按需重新生成并流式下载
 * - 在线模式：调用 getInstructionPreviewUrl/getDrawingPreviewUrl，后端按需生成后返回可访问的 URL
 * </p>
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface DesignDocService {

    /**
     * 下载指令单模板（线下模式）
     * <p>
     * 按需自动生成：若打印信息自上次生成后发生变化（或从未生成），则重新生成并覆盖/新建版本；
     * 否则直接复用已有文件，不产生新版本记录。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param response  HttpServletResponse，用于流式返回文件
     */
    void downloadInstruction(Long orderId, Long packageId, HttpServletResponse response);

    /**
     * 下载图纸模板（线下模式）
     * <p>
     * 按需自动生成：若打印信息自上次生成后发生变化（或从未生成），则重新生成并覆盖/新建版本；
     * 否则直接复用已有文件，不产生新版本记录。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param response  HttpServletResponse，用于流式返回文件
     */
    void downloadDrawing(Long orderId, Long packageId, HttpServletResponse response);

    /**
     * 获取指令单预览 URL（在线模式）
     * <p>
     * 按需自动生成：若打印信息自上次生成后发生变化（或从未生成），则重新生成并覆盖/新建版本；
     * 否则直接返回已有版本信息，不产生新版本记录。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return DocItemVO（含 id、version、fileId、templateFileUrl、isConfirmed）
     */
    DocItemVO getInstructionPreviewUrl(Long orderId, Long packageId);

    /**
     * 获取图纸预览 URL（在线模式）
     * <p>
     * 按需自动生成：若打印信息自上次生成后发生变化（或从未生成），则重新生成并覆盖/新建版本；
     * 否则直接返回已有版本信息，不产生新版本记录。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return DocItemVO（含 id、version、fileId、templateFileUrl、isConfirmed）
     */
    DocItemVO getDrawingPreviewUrl(Long orderId, Long packageId);

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
     * <p>
     * 上传后自动将 is_confirmed 置为 1（上传即视为已审阅确认）。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID（要更新的版本）
     * @param file      修订版文件
     */
    void uploadRevisedInstruction(Long orderId, Long packageId, Long id, MultipartFile file);

    /**
     * 上传修订版图纸
     * <p>
     * 上传后自动将 is_confirmed 置为 1（上传即视为已审阅确认）。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID（要更新的版本）
     * @param file      修订版文件
     */
    void uploadRevisedDrawing(Long orderId, Long packageId, Long id, MultipartFile file);

    /**
     * 确认图纸（在线模式）
     * <p>
     * 设计师预览生成的图纸满意后调用，将 is_confirmed 置为 1。
     * 若之后重新生成图纸（数据变化时触发），确认状态自动重置。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID
     */
    void confirmDrawing(Long orderId, Long packageId, Long id);

    /**
     * 确认指令单（在线模式）
     * <p>
     * 设计师确认生成的指令单内容无误后调用，将 is_confirmed 置为 1。
     * 若之后重新生成指令单（数据变化时触发），确认状态自动重置。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID
     */
    void confirmInstruction(Long orderId, Long packageId, Long id);

    /**
     * 批量查询数据包最新版指令单（packageId → DesignDocVersionVO）
     * 无记录的包不出现在结果 map 中
     *
     * @param packageIds 数据包ID集合
     * @return key=packageId，value=最新版指令单 VO
     */
    Map<Long, DesignDocVersionVO> getLatestInstructionMap(Collection<Long> packageIds);

    /**
     * 批量查询数据包最新版图纸（packageId → DesignDocVersionVO）
     * 无记录的包不出现在结果 map 中
     *
     * @param packageIds 数据包ID集合
     * @return key=packageId，value=最新版图纸 VO
     */
    Map<Long, DesignDocVersionVO> getLatestDrawingMap(Collection<Long> packageIds);
}
