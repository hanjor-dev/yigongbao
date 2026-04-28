package com.yigongbao.module.system.user.dto;

import lombok.Data;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 用户自更新 DTO（用户前台自己修改）
 * 仅允许修改手机号和头像
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class UpdateUserBySelfDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号，格式：1开头的11位手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 头像文件路径 */
    private String avatar;
}
