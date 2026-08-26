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

    /** 版本来源：AUTO=自动生成，MANUAL=手动上传 */
    private String sourceType;

    /** 模板文件ID，前端通过 GET /basic/file/download/{fileId} 下载 */
    private String templateFileId;

    /** 系统生成的模板文件访问URL */
    private String templateFileUrl;

    /** 带原始文件名响应头的短时效下载地址 */
    private String templateDownloadUrl;

    /** 修订版文件ID（可为空），前端通过 GET /basic/file/download/{fileId} 下载 */
    private String revisedFileId;

    /** 设计师上传的修订版文件访问URL（可为空） */
    private String revisedFileUrl;

    /** 带原始文件名响应头的短时效下载地址 */
    private String revisedDownloadUrl;

    /** 生成时间 */
    private LocalDateTime generateTime;

    /** 修订版上传时间（可为空） */
    private LocalDateTime revisedUploadTime;

    /** 图纸是否已确认（0=未确认，1=已确认；仅图纸版本有意义，指令单版本为 null） */
    private Integer isConfirmed;

    /** 图纸所属产品分类；指令单为空 */
    private String productCategory;

    /** 确认时间（可为空） */
    private LocalDateTime confirmTime;
}
