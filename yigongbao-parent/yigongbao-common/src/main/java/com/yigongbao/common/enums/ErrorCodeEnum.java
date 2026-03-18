package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 * 统一管理系统错误码，前端可根据错误码进行不同的处理
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端请求错误 4xx
    PARAM_ERROR(400, "参数错误"),
    MISSING_PARAMETER(400, "缺少参数：%s"),
    INVALID_PARAMETER(400, "参数无效：%s"),
    DATA_EXISTS(400, "数据已存在"),
    DATA_NOT_FOUND(404, "数据不存在"),
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(403, "没有权限访问该资源"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法"),
    REQUEST_NOT_FOUND(404, "请求路径不存在"),

    // 服务器错误 5xx
    SERVER_ERROR(500, "系统繁忙，请稍后再试"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // 业务自定义错误 6xx
    USER_NOT_FOUND(600, "用户不存在"),
    USER_DISABLED(601, "用户已被禁用"),
    USERNAME_EXISTS(602, "用户名已存在"),
    PASSWORD_ERROR(603, "密码错误"),
    TOKEN_INVALID(604, "Token无效或已过期"),
    DATA_HAS_CHILDREN(605, "该数据存在子节点，无法删除"),
    DICT_CODE_EXISTS(606, "字典编码已存在"),
    DICT_NAME_EXISTS(607, "字典名称在同一父节点下已存在"),
    ORG_NOT_FOUND(608, "机构不存在"),
    ORG_EXISTS(609, "机构名称已存在"),
    ORG_CODE_EXISTS(610, "机构编码已存在"),
    ORG_HAS_USERS(611, "该机构下存在用户，无法删除"),
    ORG_TYPE_NOT_FOUND(612, "机构类型不存在"),

    // 部门相关 613-616
    DEPT_NOT_FOUND(613, "部门不存在"),
    DEPT_EXISTS(614, "部门名称已存在"),
    DEPT_HAS_USERS(615, "该部门下存在用户，无法删除"),
    ORG_NOT_FOUND_FOR_DEPT(616, "所属机构不存在"),

    // 用户相关 617-624
    USER_EXISTS(617, "用户名已存在"),
    USER_PHONE_EXISTS(618, "手机号已存在"),
    USER_ORG_NOT_FOUND(619, "所属机构不存在"),
    USER_DEPT_NOT_FOUND(620, "所属部门不存在"),
    USER_ROLE_NOT_FOUND(621, "角色不存在"),
    ROLE_EXISTS(622, "角色编码已存在"),
    ROLE_HAS_USERS(623, "该角色下存在用户，无法删除"),
    ORG_NOT_FOUND_FOR_USER(624, "所属机构不存在"),
    OLD_PASSWORD_ERROR(625, "旧密码错误"),

    // 配置相关 626-629
    CONFIG_NOT_FOUND(626, "配置不存在"),
    CONFIG_KEY_EXISTS(627, "配置键已存在"),
    CONFIG_SYSTEM_NOT_ALLOW_UPDATE(628, "系统内置配置不可修改"),
    CONFIG_SYSTEM_NOT_ALLOW_DELETE(629, "系统内置配置不可删除");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误描述
     */
    private final String message;

    /**
     * 根据错误码获取枚举
     *
     * @param code 错误码
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static ErrorCodeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ErrorCodeEnum enumItem : ErrorCodeEnum.values()) {
            if (enumItem.getCode().equals(code)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 获取格式化后的错误描述
     *
     * @param args 格式化参数
     * @return 格式化后的错误描述
     */
    public String getMessage(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
