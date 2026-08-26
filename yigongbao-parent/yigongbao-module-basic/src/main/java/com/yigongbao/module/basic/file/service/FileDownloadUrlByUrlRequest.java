package com.yigongbao.module.basic.file.service;

/**
 * 历史业务表仅保存文件 URL 时的下载地址生成请求。
 */
public record FileDownloadUrlByUrlRequest(String fileUrl, String downloadName) {
}
