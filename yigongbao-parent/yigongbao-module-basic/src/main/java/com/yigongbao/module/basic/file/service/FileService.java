package com.yigongbao.module.basic.file.service;

import com.yigongbao.module.basic.file.vo.FileVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
     * @param bizType 业务类型（如：registration_cert、doctor_cert）
     * @return 文件信息
     */
    FileVO uploadFile(MultipartFile file, String bizType);

    /**
     * 上传并关联业务
     *
     * @param file 上传的文件
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 文件信息
     */
    FileVO uploadAndLink(MultipartFile file, String bizType, Long bizId);

    /**
     * 批量上传
     *
     * @param files 文件列表
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 文件列表
     */
    List<FileVO> uploadMultiple(MultipartFile[] files, String bizType, Long bizId);

    /**
     * 根据ID查询文件详情
     *
     * @param id 文件ID（框架雪花算法生成）
     * @return 文件信息
     */
    FileVO getById(String id);

    /**
     * 查询业务关联文件列表
     *
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @return 文件列表
     */
    List<FileVO> listByBiz(String bizType, Long bizId);

    /**
     * 下载文件到响应流
     *
     * @param id 文件ID
     * @param response HTTP 响应
     * @throws IOException IO异常
     */
    void download(String id, HttpServletResponse response) throws IOException;

    /**
     * 删除文件（同时从存储平台和数据库删除）
     *
     * @param id 文件ID
     */
    void deleteById(String id);
}
