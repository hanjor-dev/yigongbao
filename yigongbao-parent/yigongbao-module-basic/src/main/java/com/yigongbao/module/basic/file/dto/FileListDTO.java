package com.yigongbao.module.basic.file.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件列表查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class FileListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型（字典 dict_code）
     */
    private String bizType;

    /**
     * 业务ID
     */
    private Long bizId;
}
