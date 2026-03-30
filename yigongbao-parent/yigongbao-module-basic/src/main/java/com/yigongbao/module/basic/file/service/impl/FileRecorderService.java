package com.yigongbao.module.basic.file.service.impl;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.entity.FilePartDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 文件记录服务
 * 实现 x-file-storage 框架的 FileRecorder 接口，将文件上传记录持久化到数据库
 * 同时提供 toFileVO 转换方法，供业务层返回前端数据
 * <p>
 * 框架在文件上传完成后会自动调用 {@link #save(FileInfo)} 将文件信息保存到数据库
 * 业务层通过注入 {@link FileService} 使用文件管理能力
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileRecorderService extends ServiceImpl<FileDetailMapper, FileDetail> implements FileRecorder {

    private final ObjectMapper objectMapper;
    private final FilePartDetailService filePartDetailService;

    // ==================== FileRecorder 接口实现 ====================

    /**
     * 保存文件信息到数据库
     * 框架在文件上传完成后自动调用此方法
     *
     * @param info 文件信息对象
     * @return true-保存成功
     */
    @Override
    public boolean save(FileInfo info) {
        if (info == null) {
            return false;
        }
        FileDetail detail = toFileDetail(info);
        boolean success = save(detail);
        if (success) {
            info.setId(detail.getId());
            log.info("保存文件记录成功，id={}, url={}", detail.getId(), detail.getUrl());
        } else {
            log.warn("保存文件记录失败，url={}", detail.getUrl());
        }
        return success;
    }

    /**
     * 更新文件记录（手动分片上传完成时调用）
     *
     * @param info 文件信息对象
     */
    @Override
    public void update(FileInfo info) {
        if (info == null) {
            return;
        }
        FileDetail detail = toFileDetail(info);
        if (detail.getId() == null) {
            log.warn("更新文件记录失败，id 为空，url={}", detail.getUrl());
            return;
        }
        LambdaQueryWrapper<FileDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetail::getId, detail.getId());
        update(detail, wrapper);
        log.info("更新文件记录，id={}", detail.getId());
    }

    /**
     * 根据 URL 查询文件信息
     *
     * @param url 文件访问地址
     * @return 文件信息，未找到返回 null
     */
    @Override
    public FileInfo getByUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        FileDetail detail = getOne(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getUrl, url));
        return toFileInfo(detail);
    }

    /**
     * 根据 ID 查询文件信息
     *
     * @param id 文件ID
     * @return 文件信息，未找到返回 null
     */
    public FileInfo getById(String id) {
        if (StrUtil.isBlank(id)) {
            return null;
        }
        FileDetail detail = getDetailById(id);
        return toFileInfo(detail);
    }

    /**
     * 根据 ID 查询数据库实体（内部使用）
     *
     * @param id 文件ID
     * @return 数据库实体，未找到返回 null
     */
    public FileDetail getDetailById(String id) {
        if (StrUtil.isBlank(id)) {
            return null;
        }
        return baseMapper.selectById(id);
    }

    /**
     * 根据 URL 删除文件信息
     *
     * @param url 文件访问地址
     * @return true-删除成功
     */
    @Override
    public boolean delete(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        remove(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getUrl, url));
        log.info("删除文件记录，url={}", url);
        return true;
    }

    /**
     * 保存文件分片信息（断点续传）
     *
     * @param filePartInfo 分片信息
     */
    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {
        filePartDetailService.saveFilePart(filePartInfo);
    }

    /**
     * 删除文件分片信息
     *
     * @param uploadId 上传ID
     */
    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        filePartDetailService.deleteFilePartByUploadId(uploadId);
    }

    // ==================== 转换方法 ====================

    /**
     * 将 FileInfo 转为 FileDetail（用于存入数据库）
     *
     * @param info 文件信息对象
     * @return 数据库实体
     */
    public FileDetail toFileDetail(FileInfo info) {
        if (info == null) {
            return null;
        }
        FileDetail detail = new FileDetail();
        detail.setId(info.getId());
        detail.setUrl(info.getUrl());
        detail.setSize(info.getSize());
        detail.setFilename(info.getFilename());
        detail.setOriginalFilename(info.getOriginalFilename());
        detail.setBasePath(info.getBasePath());
        detail.setPath(info.getPath());
        detail.setExt(info.getExt());
        detail.setContentType(info.getContentType());
        detail.setPlatform(info.getPlatform());
        detail.setThUrl(info.getThUrl());
        detail.setThFilename(info.getThFilename());
        detail.setThSize(info.getThSize());
        detail.setThContentType(info.getThContentType());
        detail.setObjectId(info.getObjectId());
        detail.setObjectType(info.getObjectType());
        detail.setUploadId(info.getUploadId());
        detail.setUploadStatus(info.getUploadStatus());
        try {
            detail.setFileAcl(valueToJson(info.getFileAcl()));
            detail.setThFileAcl(valueToJson(info.getThFileAcl()));
            detail.setMetadata(valueToJson(info.getMetadata()));
            detail.setUserMetadata(valueToJson(info.getUserMetadata()));
            detail.setThMetadata(valueToJson(info.getThMetadata()));
            detail.setThUserMetadata(valueToJson(info.getThUserMetadata()));
            detail.setAttr(valueToJson(info.getAttr()));
            detail.setHashInfo(valueToJson(info.getHashInfo()));
        } catch (JsonProcessingException e) {
            log.warn("FileInfo JSON 序列化失败，id={}", info.getId(), e);
        }
        return detail;
    }

    /**
     * 将 FileDetail 转为 FileInfo（用于框架操作）
     *
     * @param detail 数据库实体
     * @return 文件信息对象
     */
    public FileInfo toFileInfo(FileDetail detail) {
        if (detail == null) {
            return null;
        }
        FileInfo info = new FileInfo();
        info.setId(detail.getId());
        info.setUrl(detail.getUrl());
        info.setSize(detail.getSize());
        info.setFilename(detail.getFilename());
        info.setOriginalFilename(detail.getOriginalFilename());
        info.setBasePath(detail.getBasePath());
        info.setPath(detail.getPath());
        info.setExt(detail.getExt());
        info.setContentType(detail.getContentType());
        info.setPlatform(detail.getPlatform());
        info.setThUrl(detail.getThUrl());
        info.setThFilename(detail.getThFilename());
        info.setThSize(detail.getThSize());
        info.setThContentType(detail.getThContentType());
        info.setObjectId(detail.getObjectId());
        info.setObjectType(detail.getObjectType());
        info.setUploadId(detail.getUploadId());
        info.setUploadStatus(detail.getUploadStatus());
        try {
            info.setFileAcl(jsonToObject(detail.getFileAcl()));
            info.setThFileAcl(jsonToObject(detail.getThFileAcl()));
            info.setMetadata(jsonToMetadata(detail.getMetadata()));
            info.setUserMetadata(jsonToMetadata(detail.getUserMetadata()));
            info.setThMetadata(jsonToMetadata(detail.getThMetadata()));
            info.setThUserMetadata(jsonToMetadata(detail.getThUserMetadata()));
            info.setAttr(jsonToDict(detail.getAttr()));
            info.setHashInfo(jsonToHashInfo(detail.getHashInfo()));
        } catch (Exception e) {
            log.warn("FileDetail JSON 反序列化失败，id={}", detail.getId(), e);
        }
        return info;
    }

    /**
     * 将 FileDetail 转为 FileVO（用于返回前端）
     *
     * @param detail 数据库实体
     * @return 前端视图对象
     */
    public FileVO toFileVO(FileDetail detail) {
        if (detail == null) {
            return null;
        }
        FileVO vo = new FileVO();
        vo.setId(detail.getId());
        vo.setBizType(detail.getObjectType());
        if (StrUtil.isNotBlank(detail.getObjectId())) {
            try {
                vo.setBizId(Long.parseLong(detail.getObjectId()));
            } catch (NumberFormatException ignored) {
            }
        }
        vo.setFileName(detail.getOriginalFilename());
        vo.setFilePath(detail.getPath());
        vo.setFileUrl(detail.getUrl());
        vo.setFileSize(detail.getSize());
        vo.setFileType(detail.getContentType());
        vo.setFileExt(detail.getExt());
        vo.setPlatform(detail.getPlatform());
        vo.setThUrl(detail.getThUrl());
        vo.setThSize(detail.getThSize());
        if (detail.getSize() != null) {
            vo.setFileSizeText(formatFileSize(detail.getSize()));
        }
        if (StrUtil.isNotBlank(detail.getHashInfo())) {
            try {
                HashInfo hashInfo = jsonToHashInfo(detail.getHashInfo());
                if (hashInfo != null) {
                    vo.setFileHash(hashInfo.getMd5());
                }
            } catch (Exception ignored) {
            }
        }
        vo.setCreateTime(detail.getCreateTime());
        return vo;
    }

    // ==================== JSON 序列化/反序列化 ====================

    /**
     * 将对象序列化为 JSON 字符串
     */
    public String valueToJson(Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    /**
     * 将 JSON 字符串反序列化为 Object
     */
    public Object jsonToObject(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Map（元数据）
     */
    public Map<String, String> jsonToMetadata(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Hutool Dict（附加属性）
     */
    public Dict jsonToDict(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSONUtil.toBean(json, Dict.class);
    }

    /**
     * 将 JSON 字符串反序列化为 HashInfo
     */
    public HashInfo jsonToHashInfo(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, HashInfo.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "0 B";
        }
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
