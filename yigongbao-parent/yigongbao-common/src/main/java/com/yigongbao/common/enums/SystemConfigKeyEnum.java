package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

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
    MAX_UPLOAD_SIZE("max.upload.size", "文件上传最大大小"),

    // ==================== 订单配置 ====================
    /**
     * 提交订单是否必须上传影像文件
     * true - 必须上传（默认）
     * false - 非必填
     */
    ORDER_IMAGE_REQUIRED("order.image.required", "提交订单是否必须上传影像文件"),

    /**
     * 草稿自动过期天数
     * 默认 30 天
     */
    ORDER_DRAFT_EXPIRE_DAYS("order.draft.expire.days", "草稿自动过期天数"),

    /**
     * 订单提交后修改窗口期（分钟）
     * 默认 10 分钟
     */
    ORDER_MODIFY_WINDOW_MINUTES("order.modify.window.minutes", "订单提交后修改窗口期（分钟）");

    /**
     * 配置键
     */
    private final String key;

    /**
     * 配置名称（用于日志和错误提示）
     */
    private final String name;

    /**
     * 配置值类型
     * 用于反射获取值后的类型转换
     */
    @Getter
    public enum ConfigValueType {
        STRING,
        INTEGER,
        LONG,
        BOOLEAN
    }

    /**
     * 根据配置键查找枚举
     *
     * @param key 配置键，如 "default.password"
     * @return 对应的枚举值，未找到返回 null
     */
    public static SystemConfigKeyEnum getByKey(String key) {
        if (key == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }
}
