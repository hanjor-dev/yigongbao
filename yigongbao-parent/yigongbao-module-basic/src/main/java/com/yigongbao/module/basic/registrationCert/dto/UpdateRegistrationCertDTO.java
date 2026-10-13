package com.yigongbao.module.basic.registrationCert.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 更新注册证 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class UpdateRegistrationCertDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 注册证号
     */
    private String certCode;

    /**
     * 注册证名称
     */
    private String certName;

    /**
     * 有效期开始
     */
    private LocalDate validFrom;

    /**
     * 有效期截止
     */
    private LocalDate validTo;

    /**
     * 注册证扫描件URL
     */
    private String certFileUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态（0=过期，1=有效）
     */
    private Integer status;
}
