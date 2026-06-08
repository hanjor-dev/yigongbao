package com.yigongbao.module.system.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志 Entity
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("sys_login_log")
public class LoginLogEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录方式（PASSWORD/PHONE/EMAIL）
     */
    private String loginType;

    /**
     * 登录IP
     */
    private String ip;

    /**
     * User-Agent（浏览器/设备信息）
     */
    private String userAgent;

    /**
     * IP归属地（省市信息）
     */
    private String location;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登录结果（1=成功，0=失败）
     */
    private Integer loginStatus;

    /**
     * 失败原因
     */
    private String failReason;
}
