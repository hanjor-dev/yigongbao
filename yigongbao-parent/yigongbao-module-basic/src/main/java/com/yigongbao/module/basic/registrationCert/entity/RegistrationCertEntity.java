package com.yigongbao.module.basic.registrationCert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 注册证 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("registration_cert")
@EqualsAndHashCode(callSuper = false)
public class RegistrationCertEntity extends BaseEntity implements Serializable {

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
     * 状态（0=过期，1=有效）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
