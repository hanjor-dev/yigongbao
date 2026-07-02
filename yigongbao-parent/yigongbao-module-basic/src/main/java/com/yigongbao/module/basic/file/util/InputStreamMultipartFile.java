package com.yigongbao.module.basic.file.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 InputStream 的 MultipartFile 实现
 * 用于将 InputStream 包装为 MultipartFile，以便复用现有的文件上传流程
 *
 * @author hanjor
 * @date 2026-07-02
 */
public class InputStreamMultipartFile implements MultipartFile {

    private final InputStream inputStream;
    private final long size;
    private final String filename;
    private final String contentType;

    public InputStreamMultipartFile(InputStream inputStream, long size, String filename) {
        this(inputStream, size, filename, "application/octet-stream");
    }

    public InputStreamMultipartFile(InputStream inputStream, long size, String filename, String contentType) {
        this.inputStream = inputStream;
        this.size = size;
        this.filename = filename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return inputStream.readAllBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("transferTo(File) not supported");
    }
}
