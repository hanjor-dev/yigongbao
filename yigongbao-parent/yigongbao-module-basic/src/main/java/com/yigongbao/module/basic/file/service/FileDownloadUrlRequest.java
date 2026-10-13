package com.yigongbao.module.basic.file.service;

import org.dromara.x.file.storage.core.FileInfo;

/**
 * 生成下载地址所需的文件信息和展示文件名。
 *
 * @param fileInfo        x-file-storage 文件信息
 * @param downloadName    下载时展示的文件名
 */
public record FileDownloadUrlRequest(FileInfo fileInfo, String downloadName) {
}
