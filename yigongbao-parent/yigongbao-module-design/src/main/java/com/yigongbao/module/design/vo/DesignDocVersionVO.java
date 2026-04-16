package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指令单/图纸历史版本 VO（通用）
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignDocVersionVO {

    /** 记录ID */
    private Long id;

    /** 版本号，格式 A/1, A/2, ... */
    private String version;

    /** 版本序号，用于排序 */
    private Integer versionSeq;

    /** 系统生成的模板文件访问URL */
    private String templateFileUrl;

    /** 设计师上传的修订版文件访问URL（可为空） */
    private String revisedFileUrl;

    /** 生成时间 */
    private LocalDateTime generateTime;

    /** 修订版上传时间（可为空） */
    private LocalDateTime revisedUploadTime;
}
