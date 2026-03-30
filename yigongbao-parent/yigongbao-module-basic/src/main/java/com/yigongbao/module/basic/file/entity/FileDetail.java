package com.yigongbao.module.basic.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件记录实体
 * 对应数据库表 file_detail，实现 x-file-storage 框架 FileRecorder 接口
 * 表结构与框架 FileInfo 字段完全对齐
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Data
@TableName("file_detail")
public class FileDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID（由框架雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文件访问地址
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 保存的文件名（不含路径）
     */
    private String filename;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 基础存储路径
     */
    private String basePath;

    /**
     * 存储路径（不含文件名）
     */
    private String path;

    /**
     * 文件扩展名
     */
    private String ext;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 存储平台标识（如：local、aliyun-oss-1）
     */
    private String platform;

    /**
     * 缩略图访问地址
     */
    private String thUrl;

    /**
     * 缩略图文件名
     */
    private String thFilename;

    /**
     * 缩略图大小（字节）
     */
    private Long thSize;

    /**
     * 缩略图MIME类型
     */
    private String thContentType;

    /**
     * 关联业务类型（如：registration_cert、doctor_cert）
     */
    private String objectType;

    /**
     * 关联业务ID
     */
    private String objectId;

    /**
     * 文件元数据（JSON格式）
     */
    private String metadata;

    /**
     * 用户元数据（JSON格式）
     */
    private String userMetadata;

    /**
     * 缩略图元数据（JSON格式）
     */
    private String thMetadata;

    /**
     * 缩略图用户元数据（JSON格式）
     */
    private String thUserMetadata;

    /**
     * 附加属性（JSON格式，Dict类型）
     */
    private String attr;

    /**
     * 文件ACL
     */
    private String fileAcl;

    /**
     * 缩略图ACL
     */
    private String thFileAcl;

    /**
     * 哈希信息（JSON格式，MD5/SHA256）
     */
    private String hashInfo;

    /**
     * 上传ID（手动分片上传时使用）
     */
    private String uploadId;

    /**
     * 上传状态（手动分片上传时使用，1：初始化完成，2：上传完成）
     */
    private Integer uploadStatus;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 是否删除：0-否，1-是
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}
