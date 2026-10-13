package com.yigongbao.module.basic.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分片信息实体
 * 对应数据库表 file_part_detail，仅在手动分片上传（大文件断点续传）时使用
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Data
@TableName("file_part_detail")
public class FilePartDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分片ID
     */
    private String id;

    /**
     * 存储平台
     */
    private String platform;

    /**
     * 上传ID（分片任务唯一标识）
     */
    private String uploadId;

    /**
     * 分片ETag（云存储平台返回）
     */
    private String eTag;

    /**
     * 分片号
     */
    private Integer partNumber;

    /**
     * 分片大小（字节）
     */
    private Long partSize;

    /**
     * 哈希信息（JSON格式）
     */
    private String hashInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
