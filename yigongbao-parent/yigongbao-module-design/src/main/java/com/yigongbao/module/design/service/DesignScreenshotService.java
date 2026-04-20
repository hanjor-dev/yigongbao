package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.ScreenshotVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 数据包文件截图服务接口
 *
 * @author hanjor
 * @date 2026-04-20
 */
public interface DesignScreenshotService {

    /**
     * 保存截图（upsert：有则更新 fileId，无则插入）
     *
     * @param packageId     数据包ID（用于校验 packageFileId 归属）
     * @param packageFileId 数据包文件ID
     * @param file          截图文件（PNG/JPG）
     * @return 截图 VO
     */
    ScreenshotVO saveScreenshot(Long packageId, Long packageFileId, MultipartFile file);

    /**
     * 查询截图
     *
     * @param packageId     数据包ID
     * @param packageFileId 数据包文件ID
     * @return 截图 VO，不存在返回 null
     */
    ScreenshotVO getScreenshot(Long packageId, Long packageFileId);

    /**
     * 按 packageFileId 列表批量查询截图文件ID（供 generateDrawing 使用）
     *
     * @param packageFileIds 数据包文件ID列表
     * @return packageFileId → fileId 的映射（无截图的 packageFileId 不包含在结果中）
     */
    Map<Long, String> listFileIdsByPackageFileIds(List<Long> packageFileIds);
}
