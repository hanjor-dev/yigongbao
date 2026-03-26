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
    // 用户相关 600-601
    USER_NOT_FOUND(600, "用户不存在"),
    USER_DISABLED(601, "用户已被禁用"),

    // 认证相关 602-604
    PASSWORD_ERROR(602, "密码错误"),
    TOKEN_INVALID(603, "Token无效或已过期"),
    ACCOUNT_LOCKED(604, "账户已被锁定，请%d分钟后重试"),

    // 公共业务相关 605-607
    DATA_HAS_CHILDREN(605, "该数据存在子节点，无法删除"),
    DICT_CODE_EXISTS(606, "字典编码已存在"),
    DICT_NAME_EXISTS(607, "字典名称在同一父节点下已存在"),

    // 机构相关 608-612
    ORG_NOT_FOUND(608, "机构不存在"),
    ORG_EXISTS(609, "机构名称已存在"),
    ORG_CODE_EXISTS(610, "机构编码已存在"),
    ORG_HAS_USERS(611, "该机构下存在用户，无法删除"),
    ORG_TYPE_NOT_FOUND(612, "机构类型不存在"),

    // 部门相关 613-615
    DEPT_NOT_FOUND(613, "部门不存在"),
    DEPT_EXISTS(614, "部门名称已存在"),
    DEPT_HAS_USERS(615, "该部门下存在用户，无法删除"),

    // 用户管理相关 616-622
    USER_EXISTS(616, "用户名已存在"),
    USER_PHONE_EXISTS(617, "手机号已存在"),
    USER_ORG_NOT_FOUND(618, "所属机构不存在"),
    USER_DEPT_NOT_FOUND(619, "所属部门不存在"),
    USER_ROLE_NOT_FOUND(620, "角色不存在"),
    ROLE_EXISTS(621, "角色编码已存在"),
    ROLE_HAS_USERS(622, "该角色下存在用户，无法删除"),

    // 密码相关 623
    OLD_PASSWORD_ERROR(623, "旧密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(624, "新密码不能与旧密码相同"),

    // 认证相关 625-627
    USERNAME_OR_PASSWORD_ERROR(625, "用户名或密码错误"),
    LOGIN_MAX_FAILURES(626, "登录失败次数过多，账户已被锁定"),
    PERMISSION_DENIED(627, "没有权限执行该操作"),

    // 资源相关 628-632
    RESOURCE_NOT_FOUND(628, "资源不存在"),
    RESOURCE_EXISTS(629, "资源编码已存在"),
    RESOURCE_HAS_CHILDREN(630, "该资源下存在子资源，无法删除"),
    RESOURCE_HAS_ROLES(631, "该资源已分配给角色，请先取消分配"),
    ROLE_HAS_RESOURCES(632, "该角色已分配资源，请先取消分配"),

    // 配置相关 633-636
    CONFIG_NOT_FOUND(633, "配置不存在"),
    CONFIG_KEY_EXISTS(634, "配置键已存在"),
    CONFIG_SYSTEM_NOT_ALLOW_UPDATE(635, "系统内置配置不可修改"),
    CONFIG_SYSTEM_NOT_ALLOW_DELETE(636, "系统内置配置不可删除"),

    // 医院相关 637-639
    HOSPITAL_NOT_FOUND(637, "医院不存在"),
    HOSPITAL_DISABLED(638, "医院已停用"),
    HOSPITAL_EXISTS(639, "医院名称已存在"),

    // 医院组合模板相关 640-643
    TEMPLATE_NOT_FOUND(640, "医院组合模板不存在"),
    TEMPLATE_DISABLED(641, "医院组合模板已停用"),
    TEMPLATE_EXISTS(642, "医院组合模板名称已存在"),
    TEMPLATE_HAS_USERS(643, "该模板已被用户使用，无法删除"),

    // 用户-医院关联 644
    USER_HOSPITAL_NOT_FOUND(644, "用户医院关联不存在"),

    // 部位相关 645-646
    BODY_PART_NOT_FOUND(645, "部位不存在"),
    BODY_PART_NAME_EXISTS(646, "部位名称已存在"),

    // 重建项目相关 647-648
    REBUILD_PROJECT_NOT_FOUND(647, "项目不存在"),
    REBUILD_PROJECT_NAME_EXISTS(648, "项目名称已存在"),

    // 操作日志相关 649-650
    LOG_NOT_FOUND(649, "日志记录不存在"),
    LOG_EXPORT_FAILED(650, "日志导出失败"),

    // 编码规则相关 651-655
    CODE_RULE_NOT_FOUND(651, "编码规则不存在"),
    CODE_RULE_DISABLED(652, "编码规则已禁用"),
    CODE_GENERATE_FAILED(653, "编码生成失败"),
    CODE_RULE_EXISTS(654, "规则编码已存在"),

    // 附件相关 655-660
    ATTACHMENT_NOT_FOUND(655, "附件不存在"),
    ATTACHMENT_UPLOAD_FAILED(656, "文件上传失败"),
    ATTACHMENT_DELETE_FAILED(657, "附件删除失败"),
    ATTACHMENT_TYPE_NOT_ALLOWED(658, "不支持的文件类型"),
    ATTACHMENT_SIZE_EXCEEDED(659, "文件大小超出限制"),

    // 医院科室相关 660-662
    HOSPITAL_DEPT_NOT_FOUND(660, "科室不存在"),
    HOSPITAL_DEPT_EXISTS(661, "科室名称已存在"),
    HOSPITAL_DEPT_HAS_DOCTORS(662, "该科室下存在医生，无法删除"),

    // 医生相关 663-664
    DOCTOR_NOT_FOUND(663, "医生不存在"),
    DOCTOR_EXISTS(664, "该医生已存在"),

    // 产品型号相关 665-666
    PRODUCT_NOT_FOUND(665, "产品型号不存在"),
    PRODUCT_EXISTS(666, "产品编码已存在"),

    // 注册证相关 667-668
    CERT_NOT_FOUND(667, "注册证不存在"),
    CERT_EXISTS(668, "注册证号已存在");

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
