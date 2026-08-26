package com.yigongbao.module.basic.file.service;

import com.yigongbao.module.basic.file.vo.FileVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * 文件存储服务接口
 * 为所有业务模块提供统一的文件上传、下载、删除、查询能力
 *
 * @author hanjor
 * @date 2026-03-25
 */
public interface FileService {

    /**
     * 上传文件（不关联业务）
     *
     * @param file 上传的文件
     * @param bizType 业务类型（字典 dict_code，如：10.1、10.4）
     * @return 文件信息
     */
    FileVO uploadFile(MultipartFile file, String bizType);

    /**
     * 上传 byte[] 内容（不关联业务）
     * 内部包装为 ByteArrayMultipartFile 后走统一上传流程
     *
     * @param bytes    文件内容
     * @param filename 文件名（含扩展名，用于类型校验）
     * @param bizType  业务类型（字典 dict_code）
     * @return 文件信息
     */
    FileVO uploadBytes(byte[] bytes, String filename, String bizType);

    /**
     * 上传 InputStream 流式内容（不关联业务）
     * 用于避免大文件读取到内存，支持从 FileInputStream 等流式上传
     *
     * @param inputStream 文件输入流
     * @param size        文件大小（字节）
     * @param filename    文件名（含扩展名，用于类型校验）
     * @param bizType     业务类型（字典 dict_code）
     * @return 文件信息
     */
    FileVO uploadStream(InputStream inputStream, long size, String filename, String bizType);

    /**
     * 上传并关联业务
     *
     * @param file 上传的文件
     * @param bizType 业务类型（字典 dict_code）
     * @param bizId 业务ID
     * @return 文件信息
     */
    FileVO uploadAndLink(MultipartFile file, String bizType, Long bizId);

    /**
     * 将已上传的文件关联到业务
     * 用于前端先上传文件，后端再关联业务的场景
     *
     * @param fileId  文件ID（必须是已上传的文件）
     * @param bizType 业务类型（字典 dict_code）
     * @param bizId   业务ID
     * @return 更新后的文件信息
     */
    FileVO linkFile(String fileId, String bizType, Long bizId);

    /**
     * 解除业务下某分类的所有文件关联（bizId 和 objectType 置 null）
     * 用于更新场景下先清除旧关联，再建立新关联
     *
     * @param bizType 业务类型（字典 dict_code）
     * @param bizId   业务ID
     */
    void unlinkByBiz(String bizType, Long bizId);

    /**
     * 批量上传（不关联业务）
     *
     * @param files 文件列表
     * @param bizType 业务类型（字典 dict_code）
     * @return 文件列表
     */
    List<FileVO> uploadMultiple(MultipartFile[] files, String bizType);

    /**
     * 批量上传并关联业务
     *
     * @param files 文件列表
     * @param bizType 业务类型（字典 dict_code）
     * @param bizId 业务ID
     * @return 文件列表
     */
    List<FileVO> uploadMultipleWithBizId(MultipartFile[] files, String bizType, Long bizId);

    /**
     * 根据ID查询文件详情
     *
     * @param id 文件ID（框架雪花算法生成）
     * @return 文件信息
     */
    FileVO getById(String id);

    /**
     * 根据ID列表批量查询文件
     *
     * @param ids 文件ID列表
     * @return 文件列表（不存在的ID不会出现在结果中）
     */
    List<FileVO> listByIds(List<String> ids);

    /**
     * 查询业务关联文件列表
     *
     * @param bizType 业务类型（字典 dict_code）
     * @param bizId 业务ID
     * @return 文件列表
     */
    List<FileVO> listByBiz(String bizType, Long bizId);

    /**
     * 根据历史业务表中保存的文件 URL 生成下载地址。
     * 适用于尚未保存 file_detail.id、但 URL 已由 x-file-storage 记录的兼容场景。
     */
    String generateDownloadUrl(String fileUrl, String downloadFilename);

    /**
     * 根据历史业务表中的 URL 批量生成下载地址，避免逐条查询 file_detail。
     */
    List<String> generateDownloadUrls(List<FileDownloadUrlByUrlRequest> requests);

    /**
     * 下载文件到响应流
     *
     * @param id 文件ID
     * @param response HTTP 响应
     * @throws IOException IO异常
     */
    void download(String id, HttpServletResponse response) throws IOException;

    /**
     * 使用指定的展示文件名下载文件到响应流。
     *
     * @param id 文件ID
     * @param downloadFilename 本次下载展示的文件名；为空时使用文件原始名称
     * @param response HTTP 响应
     * @throws IOException IO异常
     */
    void download(String id, String downloadFilename, HttpServletResponse response) throws IOException;

    /**
     * 下载文件内容到字节数组（供服务端内部使用，如嵌入 Excel）
     *
     * @param id 文件ID
     * @return 文件字节数组
     * @throws IOException IO异常
     */
    byte[] downloadToBytes(String id) throws IOException;

    /**
     * 删除文件（同时从存储平台和数据库删除）
     *
     * @param id 文件ID
     */
    void deleteById(String id);

    // ==================== 文件类型校验工具方法 ====================

    /**
     * 解析允许扩展名配置字符串为集合
     * 逗号分隔、trim、toLowerCase、过滤空串
     * 业务模块负责从 ConfigService 取得配置字符串后调用此方法
     *
     * @param config   配置值（可为 null/blank，此时使用 fallback）
     * @param fallback 兜底值（逗号分隔，如 ".pdf,.docx,.xlsx"）
     * @return 小写扩展名集合（含点，如 {".pdf", ".docx"}）
     */
    Set<String> parseAllowedExtensions(String config, String fallback);

    /**
     * 校验文件列表中每个文件的扩展名是否在允许集合中
     *
     * @param files        文件列表
     * @param allowedExts  允许的扩展名集合（含点，如 {".pdf", ".docx"}）
     * @param categoryName 文件类别名称（用于日志和错误提示）
     * @throws com.yigongbao.common.exception.BusinessException 任一文件类型不允许时抛出 ATTACHMENT_TYPE_NOT_ALLOWED
     */
    void assertAllFileTypesAllowed(List<FileVO> files, Set<String> allowedExts, String categoryName);

    /**
     * 批量查询文件，同时校验存在性与扩展名（一次 DB 查询）
     *
     * @param fileIds      文件 ID 列表（为空时直接返回空列表）
     * @param allowedExts  允许的扩展名集合
     * @param categoryName 文件类别名称（用于日志和错误提示）
     * @return 查询到的文件 VO 列表
     * @throws com.yigongbao.common.exception.BusinessException 任一文件不存在或类型不允许时抛出
     */
    List<FileVO> listAndValidate(List<String> fileIds, Set<String> allowedExts, String categoryName);

    /**
     * 校验文件大小是否超出限制
     * 业务模块从 ConfigService 取得最大大小（MB）配置字符串后调用此方法
     *
     * @param fileSizeBytes  文件实际大小（字节），通常来自 MultipartFile.getSize() 或 FileVO.getFileSize()
     * @param maxSizeMbStr   最大允许大小配置值（MB，字符串形式，可为 null/blank，此时使用 fallbackMb）
     * @param fallbackMb     兜底最大大小（MB）
     * @param categoryName   文件类别名称（用于日志和错误提示）
     * @throws com.yigongbao.common.exception.BusinessException 超出限制时抛出 ATTACHMENT_SIZE_EXCEEDED
     */
    void assertFileSizeAllowed(long fileSizeBytes, String maxSizeMbStr, int fallbackMb, String categoryName);
}
