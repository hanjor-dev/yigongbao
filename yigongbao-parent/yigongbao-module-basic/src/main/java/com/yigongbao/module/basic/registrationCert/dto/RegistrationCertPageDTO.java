package com.yigongbao.module.basic.registrationCert.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 注册证分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-02
 */
@Data
public class RegistrationCertPageDTO implements Serializable {

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
     * 注册证号（模糊查询）
     */
    private String certCode;

    /**
     * 注册证名称（模糊查询）
     */
    private String certName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
