package com.yigongbao.module.basic.file.service;

import org.dromara.x.file.storage.core.FileInfo;

import java.util.List;

/**
 * 统一生成对象存储文件的预签名下载地址。
 */
public interface FileDownloadUrlService {

    /**
     * 生成带 Content-Disposition 的下载地址。
     *
     * @return 下载地址；文件为空、不支持预签名或生成失败时返回 null
     */
    String generate(FileInfo fileInfo, String downloadName);

    /**
     * 批量生成下载地址。当前采用串行调用，避免创建线程池和大量并发云端签名请求；
     * 请求列表已由上层一次性完成数据库查询。
     */
    List<String> generateBatch(List<FileDownloadUrlRequest> requests);
}
