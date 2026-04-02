package com.yigongbao.module.basic.hospital.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 医院分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class HospitalPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 医院名称（模糊查询）
     */
    private String hospitalName;

    /**
     * 地区ID
     */
    private Long areaId;

    /**
     * 医院等级（字典：dict_code=3，值如 3.1/3.2）
     */
    private String hospitalLevel;

    /**
     * 医院类型（字典：dict_code=4，值如 4.1/4.2）
     */
    private String hospitalType;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
