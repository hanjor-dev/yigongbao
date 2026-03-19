package com.yigongbao.module.system.auth.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志 VO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class LoginLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
     * 登录IP
     */
    private String ip;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登录结果（1=成功，0=失败）
     */
    private Integer loginStatus;

    /**
     * 登录结果名称
     */
    private String loginStatusName;

    /**
     * 失败原因
     */
    private String failReason;
}
