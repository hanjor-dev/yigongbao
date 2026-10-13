package com.yigongbao.module.basic.registrationCert.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 注册证 VO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class RegistrationCertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 注册证号
     */
    private String certCode;

    /**
     * 注册证名称
     */
    private String certName;

    /**
     * 有效期开始日期
     */
    private LocalDate validFrom;

    /**
     * 有效期结束日期
     */
    private LocalDate validTo;

    /**
     * 注册证文件URL
     */
    private String certFileUrl;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
