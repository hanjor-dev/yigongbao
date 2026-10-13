package com.yigongbao.module.basic.file.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.basic.file.entity.FilePartDetail;
import com.yigongbao.module.basic.file.mapper.FilePartDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.springframework.stereotype.Service;

/**
 * 文件分片信息 Service
 * 仅在手动分片上传（大文件断点续传）时使用
 * 负责将分片上传的分片信息持久化到数据库
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilePartDetailService extends ServiceImpl<FilePartDetailMapper, FilePartDetail> {

    private final ObjectMapper objectMapper;

    /**
     * 保存文件分片信息
     *
     * @param info 分片信息
     */
    public void saveFilePart(FilePartInfo info) {
        if (info == null) {
            return;
        }
        FilePartDetail detail = toFilePartDetail(info);
        boolean success = save(detail);
        if (success) {
            info.setId(detail.getId());
            log.debug("保存文件分片成功，id={}, uploadId={}", detail.getId(), detail.getUploadId());
        } else {
            log.warn("保存文件分片失败，uploadId={}", detail.getUploadId());
        }
    }

    /**
     * 删除文件分片信息
     *
     * @param uploadId 上传ID
     */
    public void deleteFilePartByUploadId(String uploadId) {
        if (StrUtil.isBlank(uploadId)) {
            return;
        }
        remove(new LambdaQueryWrapper<FilePartDetail>()
                .eq(FilePartDetail::getUploadId, uploadId));
        log.debug("删除文件分片信息完成，uploadId={}", uploadId);
    }

    /**
     * 将 FilePartInfo 转为 FilePartDetail
     *
     * @param info 分片信息
     * @return 分片详情实体
     */
    private FilePartDetail toFilePartDetail(FilePartInfo info) {
        FilePartDetail detail = new FilePartDetail();
        detail.setId(info.getId());
        detail.setPlatform(info.getPlatform());
        detail.setUploadId(info.getUploadId());
        detail.setETag(info.getETag());
        detail.setPartNumber(info.getPartNumber());
        detail.setPartSize(info.getPartSize());
        detail.setHashInfo(valueToJson(info.getHashInfo()));
        return detail;
    }

    /**
     * 将对象序列化为 JSON 字符串
     */
    private String valueToJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败", e);
            return null;
        }
    }
}
