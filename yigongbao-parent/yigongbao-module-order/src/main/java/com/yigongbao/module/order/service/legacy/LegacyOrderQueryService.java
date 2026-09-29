package com.yigongbao.module.order.service.legacy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.order.dto.legacy.LegacyOrderPageDTO;
import com.yigongbao.module.order.entity.legacy.LegacyOrderListEntity;
import com.yigongbao.module.order.mapper.legacy.LegacyOrderListMapper;
import com.yigongbao.module.order.vo.legacy.LegacyOrderListVO;
import com.yigongbao.module.order.vo.legacy.LegacyOrderFilterOptionsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegacyOrderQueryService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LegacyOrderListMapper mapper;

    public IPage<LegacyOrderListVO> page(LegacyOrderPageDTO dto) {
        Page<LegacyOrderListEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<LegacyOrderListEntity> q = new LambdaQueryWrapper<>();
        q.eq(LegacyOrderListEntity::getIsDeleted, 0)
                .and(StringUtils.hasText(dto.getKeyword()), w -> w
                        .like(LegacyOrderListEntity::getSourceOrderCode, dto.getKeyword())
                        .or().like(LegacyOrderListEntity::getHospitalName, dto.getKeyword())
                        .or().like(LegacyOrderListEntity::getPatientName, dto.getKeyword())
                        .or().like(LegacyOrderListEntity::getOperatorName, dto.getKeyword()))
                .eq(StringUtils.hasText(dto.getStatus()), LegacyOrderListEntity::getStatusRaw, dto.getStatus())
                .eq(StringUtils.hasText(dto.getBusinessType()), LegacyOrderListEntity::getBusinessTypeRaw, dto.getBusinessType())
                .eq(StringUtils.hasText(dto.getPrintRequirement()), LegacyOrderListEntity::getPrintRaw, dto.getPrintRequirement())
                .like(StringUtils.hasText(dto.getDeptName()), LegacyOrderListEntity::getLegacyDeptName, dto.getDeptName())
                .like(StringUtils.hasText(dto.getHospitalName()), LegacyOrderListEntity::getHospitalName, dto.getHospitalName())
                .like(StringUtils.hasText(dto.getAreaName()), LegacyOrderListEntity::getAreaName, dto.getAreaName())
                .like(StringUtils.hasText(dto.getHospitalDeptName()), LegacyOrderListEntity::getHospitalDeptName, dto.getHospitalDeptName())
                .like(StringUtils.hasText(dto.getDoctorName()), LegacyOrderListEntity::getDoctorName, dto.getDoctorName())
                .like(StringUtils.hasText(dto.getPatientName()), LegacyOrderListEntity::getPatientName, dto.getPatientName())
                .ge(dto.getCreateTimeStart() != null, LegacyOrderListEntity::getSourceCreateTime, dto.getCreateTimeStart())
                .le(dto.getCreateTimeEnd() != null, LegacyOrderListEntity::getSourceCreateTime, dto.getCreateTimeEnd());
        applySort(q, dto.getSortField(), dto.getSortOrder());
        IPage<LegacyOrderListEntity> result = mapper.selectPage(page, q);
        Page<LegacyOrderListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<LegacyOrderListVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    public LegacyOrderFilterOptionsVO filterOptions() {
        List<String> statuses = mapper.selectObjs(new LambdaQueryWrapper<LegacyOrderListEntity>()
                        .select(LegacyOrderListEntity::getStatusRaw)
                        .eq(LegacyOrderListEntity::getIsDeleted, 0)
                        .isNotNull(LegacyOrderListEntity::getStatusRaw)
                        .ne(LegacyOrderListEntity::getStatusRaw, "")
                        .groupBy(LegacyOrderListEntity::getStatusRaw)
                        .orderByAsc(LegacyOrderListEntity::getStatusRaw))
                .stream().map(String::valueOf).toList();
        List<String> businessTypes = mapper.selectObjs(new LambdaQueryWrapper<LegacyOrderListEntity>()
                        .select(LegacyOrderListEntity::getBusinessTypeRaw)
                        .eq(LegacyOrderListEntity::getIsDeleted, 0)
                        .isNotNull(LegacyOrderListEntity::getBusinessTypeRaw)
                        .ne(LegacyOrderListEntity::getBusinessTypeRaw, "")
                        .groupBy(LegacyOrderListEntity::getBusinessTypeRaw)
                        .orderByAsc(LegacyOrderListEntity::getBusinessTypeRaw))
                .stream().map(String::valueOf).toList();
        return new LegacyOrderFilterOptionsVO(statuses, businessTypes);
    }

    private void applySort(LambdaQueryWrapper<LegacyOrderListEntity> q, String field, String order) {
        boolean asc = "ASC".equalsIgnoreCase(order);
        if ("orderCode".equals(field)) {
            if (asc) q.orderByAsc(LegacyOrderListEntity::getSourceOrderCode); else q.orderByDesc(LegacyOrderListEntity::getSourceOrderCode);
        } else if ("status".equals(field)) {
            if (asc) q.orderByAsc(LegacyOrderListEntity::getStatusRaw); else q.orderByDesc(LegacyOrderListEntity::getStatusRaw);
        } else {
            if (asc) q.orderByAsc(LegacyOrderListEntity::getSourceCreateTime); else q.orderByDesc(LegacyOrderListEntity::getSourceCreateTime);
        }
        q.orderByDesc(LegacyOrderListEntity::getId);
    }

    private LegacyOrderListVO toVO(LegacyOrderListEntity e) {
        LegacyOrderListVO v = new LegacyOrderListVO();
        // 列表对外的 id 需要保持旧医工宝订单主键 od_id，归档表自增主键不作为业务字段暴露。
        v.setId(e.getSourceOrderId());
        v.setSourceOrderId(e.getSourceOrderId());
        v.setOrderCode(e.getSourceOrderCode());
        v.setStatus(e.getStatusRaw());
        v.setBusinessTypeName(e.getBusinessTypeRaw());
        v.setNeedsPhysicalDeliveryName(e.getPrintRaw());
        v.setOrgName(e.getLegacyDeptName());
        v.setOperatorName(e.getOperatorName());
        v.setOperatorPhone(e.getOperatorPhone());
        v.setHospitalName(e.getHospitalName());
        v.setAreaName(e.getAreaName());
        v.setHospitalDeptName(e.getHospitalDeptName());
        v.setDoctorName(e.getDoctorName());
        v.setDoctorPhone(e.getDoctorPhone());
        v.setPatientName(e.getPatientName());
        v.setPatientAge(e.getPatientAgeRaw());
        v.setPatientGenderName(e.getPatientGenderRaw());
        v.setIsPostal(e.getPostalFlag());
        v.setPostalAddress(e.getPostalAddressDisplay());
        v.setDesignerName(e.getDesignerName());
        v.setExpectedDeliveryDate(format(e.getExpectedDeliveryTime(), e.getExpectedDeliveryRaw()));
        v.setDeliveryDate(format(e.getDeliveryTime(), e.getDeliveryRaw()));
        v.setEstimatedCost(e.getEstimatedCostRaw());
        v.setEstimatedCostNumber(e.getEstimatedCostNumber());
        v.setDataEvaluationOpinion(e.getDataEvaluationOpinion());
        v.setRebuildProjectSummary(e.getRebuildProjectSummary());
        v.setDesignStartTime(format(e.getDesignStartTime(), e.getDesignStartRaw()));
        v.setDesignSubmitTime(format(e.getDesignSubmitTime(), e.getDesignSubmitRaw()));
        v.setProductionStartTime(format(e.getProductionStartTime(), e.getProductionStartRaw()));
        v.setProductionEndTime(format(e.getProductionEndTime(), e.getProductionEndRaw()));
        v.setCreateTime(format(e.getSourceCreateTime(), e.getCreateTimeRaw()));
        return v;
    }

    private String format(java.time.LocalDateTime time, String raw) {
        return time != null ? time.format(FORMATTER) : raw;
    }
}
