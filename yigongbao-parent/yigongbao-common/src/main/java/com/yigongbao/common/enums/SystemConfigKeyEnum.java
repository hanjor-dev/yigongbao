package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统配置键枚举
 * 集中管理系统配置键名，避免魔法值和键名不一致问题
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Getter
@AllArgsConstructor
public enum SystemConfigKeyEnum {

    // ==================== 安全配置 ====================
    /**
     * 默认密码
     * 新用户初始密码
     */
    DEFAULT_PASSWORD("default.password", "默认密码"),

    /**
     * 最大连续登录失败次数
     * 连续失败后锁定账号
     */
    LOGIN_MAX_FAILURES("login.max.failures", "最大连续登录失败次数"),

    /**
     * 登录锁定时长（分钟）
     * 自动解锁时间
     */
    LOGIN_LOCK_DURATION("login.lock.duration", "登录锁定时长"),

    /**
     * 短信发送间隔（秒）
     * 同一手机号发送间隔
     */
    SMS_SEND_INTERVAL("sms.send.interval", "短信发送间隔"),

    // ==================== 系统配置 ====================
    /**
     * 系统名称
     */
    SYSTEM_NAME("system.name", "系统名称"),

    /**
     * 文件上传最大大小（字节）
     */
    MAX_UPLOAD_SIZE("max.upload.size", "文件上传最大大小");

    /**
     * 配置键
     */
    private final String key;

    /**
     * 配置名称（用于日志和错误提示）
     */
    private final String name;
}
