package com.yigongbao.module.system.dict.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典下拉选项查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class DictOptionsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字典类型编码
     */
    private String typeCode;
}
