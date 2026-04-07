package com.yigongbao.module.order.validator;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.basic.doctor.service.DoctorService;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dept.vo.DeptVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.service.UserHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单数据校验器
 * 统一管理订单/草稿创建时的关联数据校验逻辑
 *
 * 【校验模式说明】
 * - DRAFT: 仅校验已填写的字段（用于草稿保存，允许不完整数据）
 * - DIRECT/SUBMIT: 全量校验所有字段（用于正式订单创建）
 *
 * 【设计原则】
 * - 所有 Name 字段强制使用数据库查询结果覆盖，不信任前端传入值
 * - 仅 ID 已填写的字段才参与校验，ID 为空时跳过
 * - 禁用状态的数据视为不存在，抛出对应错误
 *
 * @author hanjor
 * @date 2026-04-03
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDataValidator {

    private final OrgService orgService;
    private final HospitalService hospitalService;
    private final DeptService deptService;
    private final DoctorService doctorService;
    private final BodyPartService bodyPartService;
    private final RebuildProjectService rebuildProjectService;
    private final UserHospitalService userHospitalService;

    /**
     * 校验模式
     */
    public enum ValidateMode {
        /**
         * 仅校验已填写的字段（草稿保存）
         */
        DRAFT,
        /**
         * 全量校验（直提创建正式订单）
         */
        DIRECT,
        /**
         * 全量校验（草稿转正式订单）
         */
        SUBMIT
    }

    // ==================== 主表校验 ====================

    /**
     * 校验并填充草稿/订单主表的关联数据
     * 根据模式决定校验范围，所有 Name 字段强制使用数据库查询结果覆盖
     *
     * 【医生处理逻辑】
     * - doctorId 有值：查询医生表，覆盖 doctorName + doctorPhone
     * - doctorId 为空 + doctorName 有值：调用 quickAdd 创建/获取医生，填充 doctorId + doctorName + doctorPhone
     * - doctorId 为空 + doctorName 为空：跳过（医生非必填）
     *
     * @param entity 实体（待填充 Name）
     * @param orgId 机构ID
     * @param hospitalId 医院ID（用于快速创建医生）
     * @param deptId 科室ID
     * @param doctorId 医生ID
     * @param doctorName 医生姓名（快速创建时传入）
     * @param doctorPhone 医生电话（快速创建时传入）
     * @param creatorId 提单人ID（用于快速创建医生时的业务员关联）
     * @param mode 校验模式
     */
    public void validateAndFillMaster(OrderDraftEntity entity,
            Long orgId, Long hospitalId, Long deptId,
            Long doctorId, String doctorName, String doctorPhone, Long creatorId,
            ValidateMode mode) {
        // required=true 表示字段必填（SUBMIT/DIRECT 模式），DRAFT 模式仅校验已填写的字段
        boolean required = (mode != ValidateMode.DRAFT);
        OrgEntity org = lookupOrg(orgId, required);
        if (org != null) entity.setOrgName(org.getOrgName());
        HospitalEntity hospital = lookupHospital(hospitalId, deptId, required);
        if (hospital != null) entity.setHospitalName(hospital.getHospitalName());
        // 校验医院范围权限（仅在 SUBMIT/DIRECT 模式校验，草稿保存跳过）
        if (required && hospitalId != null) {
            validateHospitalScope(creatorId, hospitalId);
        }
        DeptVO dept = lookupDept(deptId, required);
        if (dept != null) entity.setDeptName(dept.getDeptName());
        applyDoctorInfo(entity::setDoctorId, entity::setDoctorName, entity::setDoctorPhone,
                doctorId, doctorName, doctorPhone, hospitalId, creatorId);
    }

    /**
     * 校验并填充订单主表的关联数据（OrderMainEntity 版本）
     *
     * 【医生处理逻辑】
     * - doctorId 有值：查询医生表，覆盖 doctorName + doctorPhone
     * - doctorId 为空 + doctorName 有值：调用 quickAdd 创建/获取医生，填充 doctorId + doctorName + doctorPhone
     * - doctorId 为空 + doctorName 为空：跳过（医生非必填）
     *
     * @param entity 订单主表实体（待填充 Name）
     * @param orgId 机构ID
     * @param hospitalId 医院ID
     * @param deptId 科室ID
     * @param doctorId 医生ID
     * @param doctorName 医生姓名（快速创建时传入）
     * @param doctorPhone 医生电话（快速创建时传入）
     * @param creatorId 提单人ID
     * @param mode 校验模式
     */
    public void validateAndFillMasterForOrder(OrderMainEntity entity,
            Long orgId, Long hospitalId, Long deptId,
            Long doctorId, String doctorName, String doctorPhone, Long creatorId,
            ValidateMode mode) {
        // required=true 表示字段必填（SUBMIT/DIRECT 模式），DRAFT 模式仅校验已填写的字段
        boolean required = (mode != ValidateMode.DRAFT);
        OrgEntity org = lookupOrg(orgId, required);
        if (org != null) entity.setOrgName(org.getOrgName());
        HospitalEntity hospital = lookupHospital(hospitalId, deptId, required);
        if (hospital != null) {
            entity.setHospitalName(hospital.getHospitalName());
            entity.setAreaId(hospital.getAreaId());
            entity.setAreaName(hospital.getAreaName());
            entity.setFullAreaName(hospital.getFullAreaName());
        }
        // 校验医院范围权限（仅在 DIRECT/SUBMIT 模式校验）
        if (required && hospitalId != null) {
            validateHospitalScope(creatorId, hospitalId);
        }
        DeptVO dept = lookupDept(deptId, required);
        if (dept != null) entity.setDeptName(dept.getDeptName());
        applyDoctorInfo(entity::setDoctorId, entity::setDoctorName, entity::setDoctorPhone,
                doctorId, doctorName, doctorPhone, hospitalId, creatorId);
    }

    /**
     * 校验并填充订单明细的关联数据（OrderItemEntity 版本）
     *
     * @param entities 订单明细列表（待填充 Name）
     * @param mode 校验模式
     */
    public void validateAndFillItemsForOrder(List<OrderItemEntity> entities, ValidateMode mode) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        boolean required = (mode != ValidateMode.DRAFT);
        for (OrderItemEntity item : entities) {
            validateBodyPartForOrder(item, required);
            validateProjectForOrder(item, required);
        }
    }

    private void validateBodyPartForOrder(OrderItemEntity item, boolean required) {
        if (item.getBodyPartId() == null) {
            if (required) throw new BusinessException(ErrorCodeEnum.ORDER_BODY_PART_REQUIRED);
            return;
        }
        BodyPartDetailVO bodyPart = bodyPartService.getDetailById(item.getBodyPartId());
        if (bodyPart == null) throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
        if (bodyPart.getStatus() != null && bodyPart.getStatus().equals(StatusConstants.DISABLED)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_BODY_PART_DISABLED);
        }
        item.setBodyPartName(bodyPart.getName());
    }

    private void validateProjectForOrder(OrderItemEntity item, boolean required) {
        if (item.getProjectId() == null) {
            if (required) throw new BusinessException(ErrorCodeEnum.ORDER_PROJECT_REQUIRED);
            return;
        }
        RebuildProjectDetailVO project = rebuildProjectService.getDetailById(item.getProjectId());
        if (project == null) throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND);
        if (project.getStatus() != null && project.getStatus().equals(StatusConstants.DISABLED)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_PROJECT_DISABLED);
        }
        item.setProjectName(project.getName());
    }

    /**
     * 校验并填充订单主表的关联数据（重载版本）
     * 用于 createFromDraft 场景，直接在校验后生成 OrderMainEntity
     *
     * 【医生处理逻辑】
     * - doctorId 有值：查询医生表，覆盖 doctorName + doctorPhone
     * - doctorId 为空 + doctorName 有值：调用 quickAdd 创建/获取医生，填充 doctorId + doctorName + doctorPhone
     * - doctorId 为空 + doctorName 为空：跳过（医生非必填）
     *
     * @param orgId 机构ID
     * @param hospitalId 医院ID（快速创建医生时需要）
     * @param deptId 科室ID
     * @param doctorId 医生ID
     * @param doctorName 医生姓名（快速创建时传入）
     * @param doctorPhone 医生电话（快速创建时传入）
     * @param creatorId 提单人ID（用于快速创建医生时的业务员关联）
     * @param mode 校验模式
     * @return 包含已校验名称的 OrderMainEntity
     */
    public OrderMainEntity buildAndValidateMaster(
            Long orgId, Long hospitalId, Long deptId,
            Long doctorId, String doctorName, String doctorPhone, Long creatorId,
            ValidateMode mode) {
        OrderMainEntity entity = new OrderMainEntity();
        entity.setOrgId(orgId);
        entity.setHospitalId(hospitalId);
        entity.setDeptId(deptId);
        entity.setDoctorId(doctorId);
        // required=true 表示字段必填（SUBMIT/DIRECT 模式），DRAFT 模式仅校验已填写的字段
        boolean required = (mode != ValidateMode.DRAFT);
        OrgEntity org = lookupOrg(orgId, required);
        if (org != null) entity.setOrgName(org.getOrgName());
        HospitalEntity hospital = lookupHospital(hospitalId, deptId, required);
        if (hospital != null) {
            entity.setHospitalName(hospital.getHospitalName());
            entity.setAreaId(hospital.getAreaId());
            entity.setAreaName(hospital.getAreaName());
            entity.setFullAreaName(hospital.getFullAreaName());
        }
        // 校验医院范围权限（仅在 SUBMIT 模式校验）
        if (required && hospitalId != null) {
            validateHospitalScope(creatorId, hospitalId);
        }
        DeptVO dept = lookupDept(deptId, required);
        if (dept != null) entity.setDeptName(dept.getDeptName());
        applyDoctorInfo(entity::setDoctorId, entity::setDoctorName, entity::setDoctorPhone,
                doctorId, doctorName, doctorPhone, hospitalId, creatorId);
        return entity;
    }

    // ==================== 共享查找辅助方法（统一业务校验逻辑）====================

    /**
     * 查找并校验机构
     *
     * @param orgId 机构ID
     * @param required 是否必填
     * @return 机构实体，orgId 为 null 且 required=false 时返回 null
     */
    private OrgEntity lookupOrg(Long orgId, boolean required) {
        if (orgId == null) {
            if (required) throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
            return null;
        }
        OrgEntity org = orgService.getById(orgId);
        if (org == null) {
            log.warn("机构不存在，orgId={}", orgId);
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
        }
        if (org.getStatus() != null && org.getStatus().equals(StatusConstants.DISABLED)) {
            log.warn("机构已禁用，orgId={}", orgId);
            throw new BusinessException(ErrorCodeEnum.ORG_DISABLED);
        }
        return org;
    }

    /**
     * 查找并校验医院（含关联科室的禁用校验）
     *
     * @param hospitalId 医院ID
     * @param deptId 科室ID（用于关联校验）
     * @param required 是否必填
     * @return 医院实体，hospitalId 为 null 且 required=false 时返回 null
     */
    private HospitalEntity lookupHospital(Long hospitalId, Long deptId, boolean required) {
        if (hospitalId == null) {
            if (required) throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            return null;
        }
        HospitalEntity hospital = hospitalService.getById(hospitalId);
        if (hospital == null) {
            log.warn("医院不存在，hospitalId={}", hospitalId);
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
        }
        if (hospital.getStatus() != null && hospital.getStatus().equals(StatusConstants.DISABLED)) {
            log.warn("医院已禁用，hospitalId={}", hospitalId);
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_DISABLED);
        }
        // 校验医院关联的科室是否被禁用
        if (deptId != null) {
            DeptVO dept = deptService.getDeptById(deptId);
            if (dept == null) {
                log.warn("科室不存在，deptId={}", deptId);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
            }
            if (dept.getStatus() != null && dept.getStatus().equals(StatusConstants.DISABLED)) {
                log.warn("医院关联的科室已停用，deptId={}", deptId);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_DISABLED);
            }
        }
        return hospital;
    }

    /**
     * 查找并校验科室
     *
     * @param deptId 科室ID
     * @param required 是否必填（科室通常非必填，此参数保留扩展性）
     * @return 科室 VO，deptId 为 null 时返回 null
     */
    private DeptVO lookupDept(Long deptId, boolean required) {
        if (deptId == null) return null; // 科室非必填
        DeptVO dept = deptService.getDeptById(deptId);
        if (dept == null) {
            log.warn("科室不存在，deptId={}", deptId);
            throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
        }
        if (dept.getStatus() != null && dept.getStatus().equals(StatusConstants.DISABLED)) {
            log.warn("科室已禁用，deptId={}", deptId);
            throw new BusinessException(ErrorCodeEnum.DEPT_DISABLED);
        }
        return dept;
    }

    /**
     * 校验并应用医生信息到实体
     *
     * 【处理逻辑】
     * - doctorId 有值：查询医生表，覆盖 doctorName + doctorPhone
     * - doctorId 为空 + doctorName 有值：快速创建医生，填充 doctorId + doctorName + doctorPhone
     * - doctorId 为空 + doctorName 为空：跳过（医生非必填）
     *
     * @param setDoctorId   实体的 setDoctorId 方法引用
     * @param setDoctorName 实体的 setDoctorName 方法引用
     * @param setDoctorPhone 实体的 setDoctorPhone 方法引用
     */
    private void applyDoctorInfo(java.util.function.Consumer<Long> setDoctorId,
            java.util.function.Consumer<String> setDoctorName,
            java.util.function.Consumer<String> setDoctorPhone,
            Long doctorId, String doctorName, String doctorPhone,
            Long hospitalId, Long creatorId) {
        if (doctorId != null) {
            // 场景 A：已选择医生，校验并覆盖
            DoctorVO doctor = doctorService.getById(doctorId);
            if (doctor == null) {
                log.warn("医生不存在，doctorId={}", doctorId);
                throw new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND);
            }
            if (doctor.getStatus() != null && doctor.getStatus().equals(StatusConstants.DISABLED)) {
                log.warn("医生已禁用，doctorId={}", doctorId);
                throw new BusinessException(ErrorCodeEnum.DOCTOR_DISABLED);
            }
            setDoctorName.accept(doctor.getDoctorName());
            setDoctorPhone.accept(doctor.getDoctorPhone());
        } else if (StrUtil.isNotBlank(doctorName)) {
            // 场景 B：填写了医生姓名，快速创建
            DoctorVO doctor = quickAddDoctor(doctorName, doctorPhone, hospitalId);
            setDoctorId.accept(doctor.getId());
            setDoctorName.accept(doctor.getDoctorName());
            setDoctorPhone.accept(doctor.getDoctorPhone());
        }
        // 场景 C：医生字段全空，跳过（医生非必填）
    }

    private DoctorVO quickAddDoctor(String doctorName, String doctorPhone, Long hospitalId) {
        QuickAddDoctorDTO dto = new QuickAddDoctorDTO();
        dto.setDoctorName(doctorName);
        dto.setDoctorPhone(doctorPhone);
        dto.setHospitalId(hospitalId);
        return doctorService.quickAdd(dto);
    }


    // ==================== 明细校验 ====================

    /**
     * 校验并填充明细的关联数据
     *
     * @param entities 明细实体列表（待填充 Name）
     * @param mode 校验模式
     */
    public void validateAndFillItems(List<OrderItemDraftEntity> entities, ValidateMode mode) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        boolean required = (mode != ValidateMode.DRAFT);
        for (OrderItemDraftEntity item : entities) {
            validateBodyPart(item, required);
            validateProject(item, required);
        }
    }

    private void validateBodyPart(OrderItemDraftEntity item, boolean required) {
        if (item.getBodyPartId() == null) {
            if (required) {
                throw new BusinessException(ErrorCodeEnum.ORDER_BODY_PART_REQUIRED);
            }
            return;
        }
        BodyPartDetailVO bodyPart = bodyPartService.getDetailById(item.getBodyPartId());
        if (bodyPart == null) {
            log.warn("部位不存在，bodyPartId={}", item.getBodyPartId());
            throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
        }
        if (bodyPart.getStatus() != null && bodyPart.getStatus().equals(StatusConstants.DISABLED)) {
            log.warn("部位已禁用，bodyPartId={}", item.getBodyPartId());
            throw new BusinessException(ErrorCodeEnum.ORDER_BODY_PART_DISABLED);
        }
        item.setBodyPartName(bodyPart.getName());
    }

    private void validateProject(OrderItemDraftEntity item, boolean required) {
        if (item.getProjectId() == null) {
            if (required) {
                throw new BusinessException(ErrorCodeEnum.ORDER_PROJECT_REQUIRED);
            }
            return;
        }
        RebuildProjectDetailVO project = rebuildProjectService.getDetailById(item.getProjectId());
        if (project == null) {
            log.warn("重建项目不存在，projectId={}", item.getProjectId());
            throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND);
        }
        if (project.getStatus() != null && project.getStatus().equals(StatusConstants.DISABLED)) {
            log.warn("重建项目已禁用，projectId={}", item.getProjectId());
            throw new BusinessException(ErrorCodeEnum.ORDER_PROJECT_DISABLED);
        }
        item.setProjectName(project.getName());
    }

    /**
     * 校验草稿中已填写的部位/项目是否仍然有效（草稿模式下的数据新鲜度校验）
     * 与 validateAndFillItems 不同：本方法只检查"已填写 ID"的有效性，不强制要求字段必填
     * 用于草稿保存场景：用户选了部位/项目后草稿保存，若该数据在此后被禁用，再次保存时抛异常
     *
     * @param entities 明细实体列表
     */
    public void validateItemsForDraft(List<OrderItemDraftEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (OrderItemDraftEntity item : entities) {
            validateBodyPart(item, false);
            validateProject(item, false);
        }
    }

    /**
     * 校验用户是否有权限操作指定医院
     * 仅当 hospitalId 不为空时才执行校验
     *
     * @param userId     用户ID（可为 null，跳过校验）
     * @param hospitalId 医院ID（可为 null，跳过校验）
     */
    private void validateHospitalScope(Long userId, Long hospitalId) {
        if (userId == null || hospitalId == null) {
            return;
        }
        // 调用统一权限判断方法，内部基于 dataScopeType 校验
        if (!userHospitalService.hasPermissionOnHospital(userId, hospitalId)) {
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_SCOPE_DENIED);
        }
    }
}
