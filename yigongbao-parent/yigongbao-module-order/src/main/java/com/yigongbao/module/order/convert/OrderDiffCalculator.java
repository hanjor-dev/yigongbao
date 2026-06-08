package com.yigongbao.module.order.convert;

import cn.hutool.core.collection.CollUtil;
import com.yigongbao.common.entity.BaseEntity;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.module.basic.hospitalDept.entity.HospitalDeptEntity;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import com.yigongbao.module.system.doctor.service.DoctorService;
import com.yigongbao.module.system.doctor.vo.DoctorVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.order.dto.diff.*;
import com.yigongbao.module.order.dto.draft.OrderItemDraftItemDTO;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单修改差异计算器
 * 计算当前订单数据与修改请求之间的差异
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Component
@RequiredArgsConstructor
public class OrderDiffCalculator {

    private static final String FILE_CATEGORY_IMAGE_DATA = "10.1";
    private static final String FILE_CATEGORY_IMAGE_REPORT = "10.2";

    private final OrgService orgService;
    private final HospitalDeptService hospitalDeptService;
    private final DoctorService doctorService;

    /**
     * 计算订单修改差异
     *
     * @param currentOrder 当前订单数据
     * @param currentItems 当前订单项列表
     * @param currentFiles 当前订单文件列表
     * @param modifyDto 修改请求数据
     * @return 差异对象
     */
    public OrderModificationDiff calculateDiff(
            OrderDraftEntity currentOrder,
            List<OrderItemEntity> currentItems,
            List<OrderFileEntity> currentFiles,
            OrderModifyFullDTO modifyDto) {

        OrderModificationDiff diff = new OrderModificationDiff();

        // 1. 基础信息差异
        diff.setInfoFields(calculateBasicInfoDiff(currentOrder, modifyDto));

        // 2. 订单项差异
        diff.setItems(calculateItemsDiff(currentItems, modifyDto.getItems()));

        // 3. 文件差异
        calculateAndSetFilesDiff(diff, currentFiles, modifyDto);

        return diff;
    }

    /**
     * 计算基础信息字段差异
     */
    private List<FieldDiff> calculateBasicInfoDiff(OrderDraftEntity current, OrderModifyFullDTO dto) {
        List<FieldDiff> diffs = new ArrayList<>();

        // 患者姓名
        addDiffIfChanged(diffs, "patientName", "患者姓名",
                current.getPatientName(), dto.getPatientName());

        // 患者年龄
        addDiffIfChanged(diffs, "patientAge", "患者年龄",
                current.getPatientAge(), dto.getPatientAge());

        // 患者性别
        addDiffIfChanged(diffs, "patientGender", "患者性别",
                current.getPatientGender(), dto.getPatientGender());

        // 医院
        if (!Objects.equals(current.getHospitalId(), dto.getHospitalId())) {
            String oldDisplay = current.getHospitalName();
            OrgEntity org = orgService.getById(dto.getHospitalId());
            String newDisplay = org != null ? org.getOrgName() : "未知医院";
            diffs.add(new FieldDiff("hospitalId", "医院",
                    String.valueOf(current.getHospitalId()),
                    String.valueOf(dto.getHospitalId()),
                    oldDisplay, newDisplay));
        }

        // 医院科室
        if (!Objects.equals(current.getHospitalDeptId(), dto.getHospitalDeptId())) {
            String oldDisplay = current.getHospitalDeptName();
            HospitalDeptVO dept = hospitalDeptService.getById(dto.getHospitalDeptId());
            String newDisplay = dept != null ? dept.getHospitalDeptName() : "未知科室";
            diffs.add(new FieldDiff("hospitalDeptId", "医院科室",
                    String.valueOf(current.getHospitalDeptId()),
                    String.valueOf(dto.getHospitalDeptId()),
                    oldDisplay, newDisplay));
        }

        // 医生
        if (!Objects.equals(current.getDoctorId(), dto.getDoctorId())) {
            String oldDisplay = current.getDoctorName();
            DoctorVO doctor = doctorService.getById(dto.getDoctorId());
            String newDisplay = doctor != null ? doctor.getDoctorName() : "未知医生";
            diffs.add(new FieldDiff("doctorId", "医生",
                    String.valueOf(current.getDoctorId()),
                    String.valueOf(dto.getDoctorId()),
                    oldDisplay, newDisplay));
        }

        // 是否加急
        addDiffIfChanged(diffs, "isUrgent", "是否加急",
                current.getIsUrgent(), dto.getIsUrgent(),
                current.getIsUrgent() == StatusConstants.YES ? "是" : "否",
                dto.getIsUrgent() == StatusConstants.YES ? "是" : "否");

        // 是否邮寄
        addDiffIfChanged(diffs, "isPostal", "是否邮寄",
                current.getIsPostal(), dto.getIsPostal(),
                current.getIsPostal() == StatusConstants.YES ? "是" : "否",
                dto.getIsPostal() == StatusConstants.YES ? "是" : "否");

        // 邮寄地址
        addDiffIfChanged(diffs, "postalAddress", "邮寄地址",
                current.getPostalAddress(), dto.getPostalAddress());

        // 期望交付时间
        addDiffIfChanged(diffs, "expectedDeliveryDate", "期望交付时间",
                current.getExpectedDeliveryDate(), dto.getExpectedDeliveryDate());

        return diffs;
    }

    /**
     * 计算订单项差异
     */
    private ItemsDiff calculateItemsDiff(List<OrderItemEntity> currentItems, List<OrderItemDraftItemDTO> newItems) {
        ItemsDiff diff = new ItemsDiff();

        // 构建当前订单项的映射（通过id匹配）
        Map<Long, OrderItemEntity> currentMap = currentItems.stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        // 构建新订单项的映射（通过id匹配）
        Map<Long, OrderItemDraftItemDTO> newMap = newItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(OrderItemDraftItemDTO::getId, item -> item));

        List<ItemsDiff.OrderItemSummary> added = new ArrayList<>();
        List<ItemsDiff.OrderItemSummary> deleted = new ArrayList<>();
        List<ItemsDiff.OrderItemSummary> modified = new ArrayList<>();

        // 查找删除和修改的订单项
        for (OrderItemEntity currentItem : currentItems) {
            Long itemId = currentItem.getId();
            if (!newMap.containsKey(itemId)) {
                // 当前项在新数据中不存在，标记为删除
                deleted.add(createItemSummary(currentItem));
            } else {
                // 当前项在新数据中存在，检查是否有修改
                OrderItemDraftItemDTO newItem = newMap.get(itemId);
                if (isItemModified(currentItem, newItem)) {
                    modified.add(createItemSummary(currentItem));
                }
            }
        }

        // 查找新增的订单项（id为null或不在当前映射中）
        for (OrderItemDraftItemDTO newItem : newItems) {
            if (newItem.getId() == null || !currentMap.containsKey(newItem.getId())) {
                added.add(createItemSummaryFromDTO(newItem));
            }
        }

        diff.setAdded(added);
        diff.setDeleted(deleted);
        diff.setModified(modified);

        return diff;
    }

    /**
     * 判断订单项是否被修改
     */
    private boolean isItemModified(OrderItemEntity current, OrderItemDraftItemDTO newItem) {
        return !Objects.equals(current.getBodyPartId(), newItem.getBodyPartId())
                || !Objects.equals(current.getProjectId(), newItem.getProjectId())
                || !Objects.equals(current.getProjectDesc(), newItem.getProjectDesc())
                || !Objects.equals(current.getFormingRequirement(), newItem.getFormingRequirement())
                || !Objects.equals(current.getOtherRequirement(), newItem.getOtherRequirement());
    }

    /**
     * 从OrderItemEntity创建摘要
     */
    private ItemsDiff.OrderItemSummary createItemSummary(OrderItemEntity item) {
        ItemsDiff.OrderItemSummary summary = new ItemsDiff.OrderItemSummary();
        summary.setProjectName(item.getProjectName());
        summary.setBodyPartName(item.getBodyPartName());
        summary.setCategoryName(item.getCategoryName());
        return summary;
    }

    /**
     * 从OrderItemDraftItemDTO创建摘要
     */
    private ItemsDiff.OrderItemSummary createItemSummaryFromDTO(OrderItemDraftItemDTO item) {
        ItemsDiff.OrderItemSummary summary = new ItemsDiff.OrderItemSummary();
        summary.setProjectName(item.getProjectName());
        summary.setBodyPartName(item.getBodyPartName());
        summary.setCategoryName(null);
        return summary;
    }

    /**
     * 计算文件差异
     */
    private void calculateAndSetFilesDiff(OrderModificationDiff diff, List<OrderFileEntity> currentFiles, OrderModifyFullDTO dto) {
        // 影像资料差异
        diff.setImageData(calculateImageDiff(currentFiles, dto.getImageDataFileIds(), FILE_CATEGORY_IMAGE_DATA));
        // 影像报告差异
        diff.setImageReport(calculateImageDiff(currentFiles, dto.getImageReportFileIds(), FILE_CATEGORY_IMAGE_REPORT));
    }

    private ImageDiff calculateImageDiff(
            List<OrderFileEntity> currentFiles, List<String> newFileIds, String fileCategory) {
        // 获取当前该类别的文件ID列表
        Set<String> currentIds = currentFiles.stream()
                .filter(f -> fileCategory.equals(f.getFileCategory()))
                .map(OrderFileEntity::getFileId)
                .collect(Collectors.toSet());
        Set<String> newIds = new HashSet<>(CollUtil.emptyIfNull(newFileIds));

        // 计算新增和删除
        List<String> added = newIds.stream().filter(id -> !currentIds.contains(id)).toList();
        List<String> deleted = currentIds.stream().filter(id -> !newIds.contains(id)).toList();

        ImageDiff imageDiff = new ImageDiff();
        imageDiff.setAdded(added.isEmpty() ? null : added);
        imageDiff.setDeleted(deleted.isEmpty() ? null : deleted);
        return imageDiff;
    }

    /**
     * 辅助方法：如果值变化则添加差异记录（不带display）
     */
    private void addDiffIfChanged(List<FieldDiff> diffs, String fieldName, String fieldLabel,
                                   Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            diffs.add(new FieldDiff(fieldName, fieldLabel,
                    String.valueOf(oldValue), String.valueOf(newValue)));
        }
    }

    /**
     * 辅助方法：如果值变化则添加差异记录（带display）
     */
    private void addDiffIfChanged(List<FieldDiff> diffs, String fieldName, String fieldLabel,
                                   Object oldValue, Object newValue,
                                   String oldDisplay, String newDisplay) {
        if (!Objects.equals(oldValue, newValue)) {
            diffs.add(new FieldDiff(fieldName, fieldLabel,
                    String.valueOf(oldValue), String.valueOf(newValue),
                    oldDisplay, newDisplay));
        }
    }
}
