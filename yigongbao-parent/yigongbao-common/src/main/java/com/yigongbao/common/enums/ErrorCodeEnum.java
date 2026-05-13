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
    SUCCESS(200, "操作成功", 3),

    // ==================== 客户端请求错误 4xx ====================
    PARAM_ERROR(410, "参数错误", 5),
    MISSING_PARAMETER(410, "缺少参数：%s", 5),
    INVALID_PARAMETER(410, "参数无效：%s", 5),
    DATA_EXISTS(410, "数据已存在", 5),
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录", 1),
    FORBIDDEN(403, "没有权限访问该资源", 1),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法", 5),
    DATA_NOT_FOUND(404, "数据不存在", 5),
    REQUEST_NOT_FOUND(404, "请求路径不存在", 5),

    // ==================== 服务器错误 5xx ====================
    SERVER_ERROR(500, "系统繁忙，请稍后再试", 2),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用", 2),

    // ==================== 用户相关 601-604 ====================
    USER_NOT_FOUND(601, "用户不存在", 3),
    USER_DISABLED(602, "用户已被禁用", 3),
    USER_EXISTS(603, "用户名已存在", 3),
    USER_PHONE_EXISTS(604, "手机号已存在", 3),

    // ==================== 认证相关 605-612 ====================
    PASSWORD_ERROR(605, "密码错误", 1),
    OLD_PASSWORD_ERROR(606, "旧密码错误", 1),
    NEW_PASSWORD_SAME_AS_OLD(607, "新密码不能与旧密码相同", 3),
    USERNAME_OR_PASSWORD_ERROR(608, "用户名或密码错误", 1),
    ACCOUNT_LOCKED(609, "账户已被锁定，请%d分钟后重试", 1),
    LOGIN_MAX_FAILURES(610, "登录失败次数过多，账户已被锁定", 1),
    TOKEN_INVALID(611, "Token无效或已过期", 1),
    PERMISSION_DENIED(612, "没有权限执行该操作", 1),

    // ==================== 机构相关 613-620 ====================
    ORG_NOT_FOUND(613, "机构不存在", 4),
    ORG_DISABLED(614, "机构已禁用", 4),
    ORG_EXISTS(615, "机构名称已存在", 4),
    ORG_CODE_EXISTS(616, "机构编码已存在", 4),
    ORG_HAS_USERS(617, "该机构下存在用户，无法删除", 4),
    ORG_TYPE_NOT_FOUND(618, "机构类型不存在", 4),
    ORG_TYPE_NOT_ALLOWED(620, "不允许创建该类型机构", 4),
    ORG_CERT_FILE_REQUIRED(621, "医疗器械资质类型时资质文件必填", 4),
    ORG_DEPT_TYPE_MISMATCH(622, "关联机构类型与部门类型不匹配", 4),
    ORG_NOT_BELONG_TO_DEPT(623, "所选机构不属于该部门", 4),
    ORG_TYPE_MUST_BE_DEALER(624, "外部用户所属机构必须为经销商", 4),
    ORG_QUALIFICATION_LIMIT(625, "当前机构资质仅支持非医疗器械订单", 4),
    EMPLOYEE_NO_REQUIRED(626, "内部用户工号不能为空", 4),
    SYSTEM_CONFIG_MISSING(627, "系统配置缺失，请联系管理员", 4),

    // ==================== 部门相关 630-634 ====================
    DEPT_NOT_FOUND(630, "部门不存在", 5),
    DEPT_EXISTS(631, "部门名称已存在", 5),
    DEPT_HAS_USERS(632, "该部门下存在用户，无法删除", 5),
    DEPT_DISABLED(633, "部门已停用", 5),
    DEPT_ORG_ALREADY_BOUND(634, "该经销商已被其他外部部门关联", 5),
    DEPT_INTERNAL_ORG_LIMIT(635, "内部部门只能关联一个生产企业机构", 5),

    // ==================== 角色相关 622-627 ====================
    USER_ROLE_NOT_FOUND(622, "角色不存在", 5),
    ROLE_EXISTS(623, "角色编码已存在", 5),
    ROLE_HAS_USERS(624, "该角色下存在用户，无法删除", 5),
    ROLE_HAS_RESOURCES(625, "该角色已分配资源，请先取消分配", 5),
    ROLE_NOT_FOUND(626, "角色不存在", 5),
    USER_ROLE_EXISTS(627, "用户已拥有该角色", 5),

    // ==================== 用户关联 628-630 ====================
    USER_ORG_NOT_FOUND(628, "所属机构不存在", 4),
    USER_DEPT_NOT_FOUND(629, "所属部门不存在", 5),
    USER_HOSPITAL_NOT_FOUND(630, "用户医院关联不存在", 4),

    // ==================== 用户创建相关 631-634 ====================
    USER_ROLE_HOSPITAL_SCOPE_REQUIRED(631, "角色数据权限为医院范围，请选择可操作的医院", 3),
    USER_HOSPITAL_INVALID(632, "存在无效的医院ID", 3),
    USER_ROLE_SPECIALTY_REQUIRED(633, "设计师角色必须选择专业方向", 3),
    USER_SPECIALTY_INVALID(634, "专业方向无效，请重新选择：%s", 3),
    USER_PASSWORD_WEAK(635, "密码必须包含字母和数字，长度6-20位", 3),

    // ==================== 邮箱相关 636 ====================
    USER_EMAIL_EXISTS(636, "邮箱已存在", 3),

    // ==================== 验证码相关 637-641 ====================
    CAPTCHA_TOO_FREQUENT(637, "发送过于频繁，请稍后再试", 5),
    CAPTCHA_DAILY_LIMIT(638, "今日发送次数已达上限", 5),
    CAPTCHA_EXPIRED(639, "验证码已过期或不存在", 5),
    CAPTCHA_ERROR(640, "验证码错误", 5),
    CAPTCHA_TYPE_INVALID(641, "不支持的验证码类型", 5),

    // ==================== 资源相关 631-634 ====================
    RESOURCE_NOT_FOUND(631, "资源不存在", 5),
    RESOURCE_EXISTS(632, "资源编码已存在", 5),
    RESOURCE_HAS_CHILDREN(633, "该资源下存在子资源，无法删除", 5),
    RESOURCE_HAS_ROLES(634, "该资源已分配给角色，请先取消分配", 5),

    // ==================== 配置相关 635-638 ====================
    CONFIG_NOT_FOUND(635, "配置不存在", 5),
    CONFIG_KEY_EXISTS(636, "配置键已存在", 5),
    CONFIG_SYSTEM_NOT_ALLOW_UPDATE(637, "系统内置配置不可修改", 5),
    CONFIG_SYSTEM_NOT_ALLOW_DELETE(638, "系统内置配置不可删除", 5),

    // ==================== 字典相关 639-640 ====================
    DICT_CODE_EXISTS(639, "字典编码已存在", 5),
    DICT_NAME_EXISTS(640, "字典名称在同一父节点下已存在", 5),

    // ==================== 数据约束 641 ====================
    DATA_HAS_CHILDREN(641, "该数据存在子节点，无法删除", 5),

    // ==================== 医院相关 642-645 ====================
    HOSPITAL_NOT_FOUND(642, "医院不存在", 4),
    HOSPITAL_DISABLED(643, "医院已停用", 4),
    HOSPITAL_EXISTS(644, "医院名称已存在", 4),
    HOSPITAL_CODE_EXISTS(645, "医院编码已存在", 4),

    // ==================== 医院科室 646-647 ====================
    HOSPITAL_DEPT_NOT_FOUND(646, "科室不存在", 4),
    HOSPITAL_DEPT_EXISTS(647, "科室已存在", 4),
    HOSPITAL_DEPT_DISABLED(6460, "科室已停用", 4),

    // ==================== 产品 648-649 ====================
    PRODUCT_NOT_FOUND(648, "产品不存在", 4),
    PRODUCT_EXISTS(649, "产品名称已存在", 4),

    // ==================== 部位相关 650-651 ====================
    BODY_PART_NOT_FOUND(650, "部位不存在", 4),
    BODY_PART_NAME_EXISTS(651, "部位名称已存在", 4),

    // ==================== 重建项目 652-653 ====================
    REBUILD_PROJECT_NOT_FOUND(652, "项目不存在", 4),
    REBUILD_PROJECT_NAME_EXISTS(653, "项目名称已存在", 4),

    // ==================== 注册证 654-655 ====================
    CERT_NOT_FOUND(654, "注册证不存在", 4),
    CERT_EXISTS(655, "注册证号已存在", 4),

    // ==================== 模板相关 656-659 ====================
    TEMPLATE_NOT_FOUND(656, "医院组合模板不存在", 4),
    TEMPLATE_DISABLED(657, "医院组合模板已停用", 4),
    TEMPLATE_EXISTS(658, "医院组合模板名称已存在", 4),
    TEMPLATE_HAS_USERS(659, "该模板已被用户使用，无法删除", 4),

    // ==================== 附件相关 660-665 ====================
    ATTACHMENT_NOT_FOUND(660, "附件不存在", 4),
    ATTACHMENT_UPLOAD_FAILED(661, "文件上传失败", 4),
    ATTACHMENT_DELETE_FAILED(662, "附件删除失败", 4),
    ATTACHMENT_TYPE_NOT_ALLOWED(663, "不支持的文件类型", 4),
    ATTACHMENT_SIZE_EXCEEDED(664, "文件大小超出限制", 4),
    ATTACHMENT_FILENAME_ILLEGAL(665, "文件名包含非法字符", 4),

    // ==================== 操作日志 666-667 ====================
    LOG_NOT_FOUND(666, "日志记录不存在", 5),
    LOG_EXPORT_FAILED(667, "日志导出失败", 5),

    // ==================== 编码规则 668-671 ====================
    CODE_RULE_NOT_FOUND(668, "编码规则不存在", 4),
    CODE_RULE_DISABLED(669, "编码规则已禁用", 4),
    CODE_GENERATE_FAILED(670, "编码生成失败", 4),
    CODE_RULE_EXISTS(671, "规则编码已存在", 4),
    CODE_SEQ_OVERFLOW(673, "编码序号超出最大限制，请联系管理员", 4),

    // ==================== 医生相关 674-676 ====================
    DOCTOR_NOT_FOUND(674, "医生不存在", 4),
    DOCTOR_DISABLED(675, "医生已被禁用", 4),
    DOCTOR_EXISTS(676, "医生编码已存在", 4),

    // ==================== 订单相关 677-714 ====================
    // 基础错误
    ORDER_NOT_FOUND(677, "订单不存在", 3),
    ORDER_DRAFT_NOT_FOUND(678, "草稿不存在", 3),

    // 状态错误
    ORDER_STATUS_ERROR(679, "订单状态不合法", 3),
    ORDER_STATUS_TRANSITION_ERROR(680, "订单状态转换不合法", 3),
    ORDER_NOT_DRAFT(681, "只有草稿状态的订单才能操作", 3),
    ORDER_CANNOT_DELETE(682, "只有草稿状态的订单才能删除", 3),
    ORDER_WITHDRAW_NOT_ALLOWED(683, "当前状态不允许撤回", 3),
    ORDER_RESUBMIT_NOT_ALLOWED(684, "当前状态不允许重新提交", 3),
    ORDER_ALREADY_SUBMITTED(685, "订单已提交，不能重复提交", 3),
    ORDER_ALREADY_AUDITED(686, "订单已审核，不能重复操作", 3),
    ORDER_NOT_WITHIN_WINDOW(687, "订单已超过10分钟修改窗口期", 3),

    // 草稿相关
    ORDER_DRAFT_EXPIRED(688, "草稿已过期，请重新创建", 3),
    ORDER_DRAFT_NOT_MINE(689, "只能查看自己的草稿", 3),
    ORDER_DRAFT_ALREADY_SUBMITTED(690, "草稿已提交，不能重复提交", 3),

    // 文件相关
    ORDER_FILE_NOT_FOUND(691, "文件不存在：%s", 3),
    ORDER_FILE_REQUIRED(692, "请上传必需的文件：%s", 3),
    ORDER_FILE_CATEGORY_ERROR(693, "文件类别不合法", 3),

    // 明细相关
    ORDER_ITEM_NOT_FOUND(694, "订单明细不存在", 3),
    ORDER_ITEM_REQUIRED(695, "请至少添加一个重建项目", 3),
    ORDER_ITEM_EMPTY(696, "重建项目明细不能为空", 3),

    // 类型相关
    ORDER_TYPE_INVALID(697, "订单类型不合法", 3),
    ORDER_BUSINESS_TYPE_INVALID(698, "业务类型不合法", 3),
    ORDER_PATIENT_GENDER_INVALID(699, "患者性别不合法", 3),

    // 是否需要实体交付相关
    ORDER_NEEDS_PHYSICAL_DELIVERY_INVALID(700, "是否需要实体交付值不合法", 3),
    ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN(701, "需要实体交付的订单不允许修改为不需要实体交付", 3),

    // 审核相关
    ORDER_AUDIT_REMARK_REQUIRED(702, "审核驳回时必须填写驳回原因", 3),

    // 修改申请相关
    ORDER_MODIFY_APPLY_NOT_FOUND(703, "修改申请不存在", 3),
    ORDER_MODIFY_APPLY_STATUS_ERROR(704, "修改申请状态不合法", 3),
    ORDER_MODIFY_APPLY_ALREADY_PROCESSED(705, "该修改申请已处理", 3),

    // 状态机循环限制
    ORDER_EXCESSIVE_AUDIT_REJECT(706, "审核驳回次数超过上限（%s次），请联系管理员处理", 3),
    ORDER_EXCESSIVE_REWORK(707, "返工次数超过上限（%s次），请联系管理员处理", 3),
    ORDER_EXCESSIVE_DESIGN_REJECT(708, "设计审核驳回次数超过上限（%s次），请联系管理员处理", 3),

    // 订单项目/权限
    ORDER_PROJECT_DISABLED(709, "重建项目已禁用，不可下单", 3),
    HOSPITAL_SCOPE_DENIED(710, "无权操作该医院的订单", 3),

    // 订单部位/项目必填
    ORDER_BODY_PART_REQUIRED(711, "重建部位不能为空", 3),
    ORDER_BODY_PART_DISABLED(712, "重建部位已禁用，不可下单", 3),
    ORDER_PROJECT_REQUIRED(713, "重建项目不能为空", 3),

    // 导出
    ORDER_EXPORT_FAILED(714, "订单导出失败", 3),

    // 修改申请新增错误码（715 起）
    ORDER_MODIFY_APPLY_EXISTS(715, "已有待审核的修改申请，请等待处理后再发起新申请", 3),
    ORDER_MODIFY_FIELD_NOT_ALLOWED(716, "该字段不在申请范围内，请检查申请类型", 3),
    ORDER_NOT_APPLICABLE_STATUS(717, "当前订单状态不适用修改申请功能", 3),
    ORDER_MODIFY_APPLY_NOT_MINE(718, "只能撤回自己的申请", 3),
    ORDER_MODIFY_REJECT_REASON_REQUIRED(719, "审核拒绝时必须填写驳回原因", 3),
    ORDER_MODIFY_AUDIT_PERMISSION_DENIED(720, "只有管理员可以审核申请", 3),
    ORDER_MODIFY_FIELD_CONFIG_NOT_FOUND(721, "字段配置不存在，请联系管理员", 5),
    ORDER_MODIFY_TYPE_NOT_ALLOWED_IN_PHASE(722, "当前阶段不允许申请该类型的修改", 3),

    // ==================== 设计师分配（723-729）====================
    DESIGNER_NOT_FOUND(723, "设计师不存在", 3),
    DESIGNER_ROLE_INVALID(724, "用户角色不是设计师或设计师管理员", 3),
    DESIGNER_DISABLED(725, "设计师已被禁用", 3),
    DESIGNER_SPECIALTY_MISMATCH(726, "设计师专业方向与订单项目不一致", 3),
    ORDER_DESIGNER_MISMATCH(727, "非分配设计师，无权操作此订单", 3),

    // ==================== 修改申请阻断（730-731）====================
    ORDER_HAS_PENDING_MODIFY_APPLY(730, "订单存在待审核的修改申请，请先处理后再操作", 3),
    ORDER_HAS_APPROVED_MODIFY_APPLY(731, "订单存在已批准但未执行的修改申请，请先执行或撤销后再操作", 3),

    // ==================== 修改执行校验（732）====================
    ORDER_MODIFY_INCOMPLETE(732, "申请类型 {0} 未提供修改内容，请补充后重新提交", 3),

    // ==================== 产品规格相关 754-756 ====================
    PRODUCT_HAS_SPECS(754, "产品下存在规格，无法删除", 4),
    PRODUCT_SPEC_NOT_FOUND(755, "产品规格不存在", 4),
    PRODUCT_SPEC_EXISTS(756, "同一产品下规格名称已存在", 4),
    PRODUCT_SPEC_IN_USE(757, "规格已被打印信息引用，无法删除", 4),

    // ==================== 设计阶段（758-762）====================
    DESIGN_PACKAGE_NOT_FOUND(758, "数据包不存在", 3),
    DESIGN_PACKAGE_HAS_PRODUCTS(759, "数据包已关联打印产品，无法删除", 3),
    DESIGN_PACKAGE_HAS_DOCS(760, "数据包已生成指令单或图纸，无法删除", 3),
    DESIGN_MODEL_NOT_FOUND(761, "可视化模型不存在", 3),
    DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED(762, "不支持的压缩包格式", 3),

    // ==================== 设计文档生成（763-769）====================
    DESIGN_ARCHIVE_PARSE_FAILED(763, "压缩包解析失败：%s", 3),
    DESIGN_ARCHIVE_EMPTY(764, "压缩包中无有效文件", 3),
    DESIGN_ORDER_STATUS_NOT_ALLOWED(765, "当前工单状态不允许此操作", 3),
    DESIGN_OPERATOR_NOT_ALLOWED(766, "非当前设计师，无权操作此工单", 3),
    PRINT_INFO_REQUIRED(767, "请先填写数据包的打印信息，再生成指令单和图纸", 3),
    DOC_VERSION_NOT_FOUND(768, "指定版本的文档不存在", 3),
    DESIGN_PACKAGE_FILE_NOT_FOUND(769, "数据包文件不存在", 3),
    DESIGN_PACKAGE_FILE_WRONG_PACKAGE(770, "数据包文件不属于指定数据包", 3),
    DESIGN_SUBMIT_CHECK_FAILED(771, "提交校验未通过：%s", 3),
    DESIGNER_NOT_ASSIGNED(772, "订单未分配设计师，无法进行此操作", 3),

    // ==================== 图形验证码 773-776 ====================
    CAPTCHA_GRAPHIC_EXPIRED(773, "行为验证已过期，请刷新重试", 5),
    CAPTCHA_GRAPHIC_ERROR(774, "行为验证失败", 5),
    CAPTCHA_TOKEN_MISSING(775, "请先完成行为验证", 5),
    CAPTCHA_TOKEN_INVALID(776, "请重新进行行为验证", 5),

    // ==================== 用户名生成相关 777-780 ====================
    ORG_USERNAME_PREFIX_MISSING(777, "机构未配置账号前缀，无法自动生成用户名", 5),
    USER_USERNAME_REQUIRED(778, "手动输入模式下用户名不能为空", 5),
    ORG_USERNAME_PREFIX_EXISTS(779, "账号前缀已存在", 5),
    ORG_USERNAME_PREFIX_NOT_ALLOWED(780, "账号前缀创建后不允许修改", 5),

    // ==================== 限流 781 ====================
    RATE_LIMIT_EXCEEDED(429, "操作过于频繁，请稍后再试", 2),

    // ==================== 签名验证 782-785 ====================
    SIGN_PARAM_MISSING(782, "请求不合法", 2),
    SIGN_TIMESTAMP_EXPIRED(783, "请求已过期，请检查系统时间", 2),
    SIGN_NONCE_USED(784, "重复请求", 2),
    SIGN_INVALID(785, "签名验证失败", 2);

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误描述
     */
    private final String message;

    /**
     * 错误优先级（1=最高优先级，5=最低优先级）
     */
    private final Integer priority;

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
