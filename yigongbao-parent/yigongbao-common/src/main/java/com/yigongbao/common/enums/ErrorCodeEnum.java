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

    // ==================== 成功 ====================
    SUCCESS(200, "操作成功"),

    // ==================== 客户端请求错误 4xx ====================
    PARAM_ERROR(400, "参数错误"),
    MISSING_PARAMETER(400, "缺少参数：%s"),
    INVALID_PARAMETER(400, "参数无效：%s"),
    DATA_EXISTS(400, "数据已存在"),
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(403, "没有权限访问该资源"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法"),
    DATA_NOT_FOUND(404, "数据不存在"),
    REQUEST_NOT_FOUND(404, "请求路径不存在"),

    // ==================== 服务器错误 5xx ====================
    SERVER_ERROR(500, "系统繁忙，请稍后再试"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // ==================== 用户相关 601-604 ====================
    USER_NOT_FOUND(601, "用户不存在"),
    USER_DISABLED(602, "用户已被禁用"),
    USER_EXISTS(603, "用户名已存在"),
    USER_PHONE_EXISTS(604, "手机号已存在"),

    // ==================== 认证相关 605-612 ====================
    PASSWORD_ERROR(605, "密码错误"),
    OLD_PASSWORD_ERROR(606, "旧密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(607, "新密码不能与旧密码相同"),
    USERNAME_OR_PASSWORD_ERROR(608, "用户名或密码错误"),
    ACCOUNT_LOCKED(609, "账户已被锁定，请%d分钟后重试"),
    LOGIN_MAX_FAILURES(610, "登录失败次数过多，账户已被锁定"),
    TOKEN_INVALID(611, "Token无效或已过期"),
    PERMISSION_DENIED(612, "没有权限执行该操作"),

    // ==================== 机构相关 613-618 ====================
    ORG_NOT_FOUND(613, "机构不存在"),
    ORG_DISABLED(614, "机构已禁用"),
    ORG_EXISTS(615, "机构名称已存在"),
    ORG_CODE_EXISTS(616, "机构编码已存在"),
    ORG_HAS_USERS(617, "该机构下存在用户，无法删除"),
    ORG_TYPE_NOT_FOUND(618, "机构类型不存在"),

    // ==================== 部门相关 619-621 ====================
    DEPT_NOT_FOUND(619, "部门不存在"),
    DEPT_EXISTS(620, "部门名称已存在"),
    DEPT_HAS_USERS(621, "该部门下存在用户，无法删除"),

    // ==================== 角色相关 622-627 ====================
    USER_ROLE_NOT_FOUND(622, "角色不存在"),
    ROLE_EXISTS(623, "角色编码已存在"),
    ROLE_HAS_USERS(624, "该角色下存在用户，无法删除"),
    ROLE_HAS_RESOURCES(625, "该角色已分配资源，请先取消分配"),
    ROLE_NOT_FOUND(626, "角色不存在"),
    USER_ROLE_EXISTS(627, "用户已拥有该角色"),

    // ==================== 用户关联 628-630 ====================
    USER_ORG_NOT_FOUND(628, "所属机构不存在"),
    USER_DEPT_NOT_FOUND(629, "所属部门不存在"),
    USER_HOSPITAL_NOT_FOUND(630, "用户医院关联不存在"),

    // ==================== 资源相关 631-634 ====================
    RESOURCE_NOT_FOUND(631, "资源不存在"),
    RESOURCE_EXISTS(632, "资源编码已存在"),
    RESOURCE_HAS_CHILDREN(633, "该资源下存在子资源，无法删除"),
    RESOURCE_HAS_ROLES(634, "该资源已分配给角色，请先取消分配"),

    // ==================== 配置相关 635-638 ====================
    CONFIG_NOT_FOUND(635, "配置不存在"),
    CONFIG_KEY_EXISTS(636, "配置键已存在"),
    CONFIG_SYSTEM_NOT_ALLOW_UPDATE(637, "系统内置配置不可修改"),
    CONFIG_SYSTEM_NOT_ALLOW_DELETE(638, "系统内置配置不可删除"),

    // ==================== 字典相关 639-640 ====================
    DICT_CODE_EXISTS(639, "字典编码已存在"),
    DICT_NAME_EXISTS(640, "字典名称在同一父节点下已存在"),

    // ==================== 数据约束 641 ====================
    DATA_HAS_CHILDREN(641, "该数据存在子节点，无法删除"),

    // ==================== 医院相关 642-645 ====================
    HOSPITAL_NOT_FOUND(642, "医院不存在"),
    HOSPITAL_DISABLED(643, "医院已停用"),
    HOSPITAL_EXISTS(644, "医院名称已存在"),
    HOSPITAL_CODE_EXISTS(645, "医院编码已存在"),

    // ==================== 医院科室 646-647 ====================
    HOSPITAL_DEPT_NOT_FOUND(646, "科室不存在"),
    HOSPITAL_DEPT_EXISTS(647, "科室已存在"),

    // ==================== 产品型号 648-649 ====================
    PRODUCT_NOT_FOUND(648, "产品型号不存在"),
    PRODUCT_EXISTS(649, "产品编码已存在"),

    // ==================== 部位相关 650-651 ====================
    BODY_PART_NOT_FOUND(650, "部位不存在"),
    BODY_PART_NAME_EXISTS(651, "部位名称已存在"),

    // ==================== 重建项目 652-653 ====================
    REBUILD_PROJECT_NOT_FOUND(652, "项目不存在"),
    REBUILD_PROJECT_NAME_EXISTS(653, "项目名称已存在"),

    // ==================== 注册证 654-655 ====================
    CERT_NOT_FOUND(654, "注册证不存在"),
    CERT_EXISTS(655, "注册证号已存在"),

    // ==================== 模板相关 656-659 ====================
    TEMPLATE_NOT_FOUND(656, "医院组合模板不存在"),
    TEMPLATE_DISABLED(657, "医院组合模板已停用"),
    TEMPLATE_EXISTS(658, "医院组合模板名称已存在"),
    TEMPLATE_HAS_USERS(659, "该模板已被用户使用，无法删除"),

    // ==================== 附件相关 660-665 ====================
    ATTACHMENT_NOT_FOUND(660, "附件不存在"),
    ATTACHMENT_UPLOAD_FAILED(661, "文件上传失败"),
    ATTACHMENT_DELETE_FAILED(662, "附件删除失败"),
    ATTACHMENT_TYPE_NOT_ALLOWED(663, "不支持的文件类型"),
    ATTACHMENT_SIZE_EXCEEDED(664, "文件大小超出限制"),
    ATTACHMENT_FILENAME_ILLEGAL(665, "文件名包含非法字符"),

    // ==================== 操作日志 666-667 ====================
    LOG_NOT_FOUND(666, "日志记录不存在"),
    LOG_EXPORT_FAILED(667, "日志导出失败"),

    // ==================== 编码规则 668-671 ====================
    CODE_RULE_NOT_FOUND(668, "编码规则不存在"),
    CODE_RULE_DISABLED(669, "编码规则已禁用"),
    CODE_GENERATE_FAILED(670, "编码生成失败"),
    CODE_RULE_EXISTS(671, "规则编码已存在"),

    // ==================== 医生相关 672-674 ====================
    DOCTOR_NOT_FOUND(672, "医生不存在"),
    DOCTOR_DISABLED(673, "医生已被禁用"),
    DOCTOR_EXISTS(674, "医生编码已存在"),

    // ==================== 订单相关 675-701 ====================
    // 基础错误
    ORDER_NOT_FOUND(675, "订单不存在"),
    ORDER_DRAFT_NOT_FOUND(676, "草稿不存在"),

    // 状态错误
    ORDER_STATUS_ERROR(677, "订单状态不合法"),
    ORDER_STATUS_TRANSITION_ERROR(678, "订单状态转换不合法"),
    ORDER_NOT_DRAFT(679, "只有草稿状态的订单才能操作"),
    ORDER_CANNOT_DELETE(680, "只有草稿状态的订单才能删除"),
    ORDER_WITHDRAW_NOT_ALLOWED(681, "当前状态不允许撤回"),
    ORDER_RESUBMIT_NOT_ALLOWED(682, "当前状态不允许重新提交"),
    ORDER_ALREADY_SUBMITTED(683, "订单已提交，不能重复提交"),
    ORDER_ALREADY_AUDITED(684, "订单已审核，不能重复操作"),
    ORDER_NOT_WITHIN_WINDOW(685, "订单已超过10分钟修改窗口期"),

    // 草稿相关
    ORDER_DRAFT_EXPIRED(686, "草稿已过期，请重新创建"),
    ORDER_DRAFT_NOT_MINE(687, "只能查看自己的草稿"),
    ORDER_DRAFT_ALREADY_SUBMITTED(688, "草稿已提交，不能重复提交"),

    // 文件相关
    ORDER_FILE_NOT_UPLOADED(689, "订单文件未上传"),
    ORDER_FILE_REQUIRED(690, "请上传必需的文件：%s"),
    ORDER_FILE_CATEGORY_ERROR(691, "文件类别不合法"),

    // 明细相关
    ORDER_ITEM_NOT_FOUND(692, "订单明细不存在"),
    ORDER_ITEM_REQUIRED(693, "请至少添加一个重建项目"),
    ORDER_ITEM_EMPTY(694, "重建项目明细不能为空"),

    // 类型相关
    ORDER_TYPE_INVALID(695, "订单类型不合法"),
    ORDER_BUSINESS_TYPE_INVALID(696, "业务类型不合法"),
    ORDER_PATIENT_GENDER_INVALID(697, "患者性别不合法"),

    // 审核相关
    ORDER_AUDIT_REMARK_REQUIRED(698, "审核驳回时必须填写驳回原因"),

    // 修改申请相关
    ORDER_MODIFY_APPLY_NOT_FOUND(699, "修改申请不存在"),
    ORDER_MODIFY_APPLY_STATUS_ERROR(700, "修改申请状态不合法"),
    ORDER_MODIFY_APPLY_ALREADY_PROCESSED(701, "该修改申请已处理");

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
