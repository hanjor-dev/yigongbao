package com.yigongbao.module.order.dto.modify;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.yigongbao.module.order.constant.OrderModifyObjectType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全量修改配置 DTO
 * 支持按阶段配置允许修改的业务对象
 *
 * @author hanjor
 * @date 2026-05-22
 */
@Data
@Slf4j
public class ModifyFullConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 阶段配置映射（ORDER/DESIGN -> PhaseConfig）
     */
    private Map<String, PhaseConfig> phases = new HashMap<>();

    @JsonAnySetter
    public void setPhase(String phaseName, PhaseConfig config) {
        this.phases.put(phaseName, config);
    }

    /**
     * 阶段配置
     */
    @Data
    public static class PhaseConfig implements Serializable {
        /**
         * 允许修改的对象列表
         */
        private List<String> allowedObjects;

        /**
         * 对象配置映射（patient/doctor/hospital/delivery/items/images -> ObjectConfig）
         */
        private Map<String, ObjectConfig> objects;
    }

    /**
     * 对象配置
     */
    @Data
    public static class ObjectConfig implements Serializable {
        /**
         * 对象名称（用于日志）
         */
        private String label;

        /**
         * 简单对象字段列表（patient/doctor/hospital/delivery）
         */
        private List<String> fields;

        /**
         * 重建项目核心字段（items）
         */
        private List<String> coreFields;

        /**
         * 重建项目描述字段（items）
         */
        private List<String> descFields;
    }

    /**
     * 从 JSON 字符串解析配置
     */
    public static ModifyFullConfigDTO parseFromJson(String json) {
        if (StrUtil.isBlank(json)) {
            log.warn("配置为空，使用默认配置");
            return getDefaultConfig();
        }

        try {
            return JSONUtil.toBean(json, ModifyFullConfigDTO.class);
        } catch (Exception e) {
            log.error("解析配置失败，使用默认配置: json={}", json, e);
            return getDefaultConfig();
        }
    }

    /**
     * 获取默认配置
     */
    private static ModifyFullConfigDTO getDefaultConfig() {
        ModifyFullConfigDTO config = new ModifyFullConfigDTO();

        // ORDER 阶段配置
        PhaseConfig orderPhase = new PhaseConfig();
        orderPhase.setAllowedObjects(Arrays.asList(
            OrderModifyObjectType.PATIENT,
            OrderModifyObjectType.DOCTOR,
            OrderModifyObjectType.HOSPITAL,
            OrderModifyObjectType.DELIVERY,
            OrderModifyObjectType.ITEMS,
            OrderModifyObjectType.IMAGES
        ));
        orderPhase.setObjects(new HashMap<>());

        // 患者信息
        ObjectConfig patient = new ObjectConfig();
        patient.setLabel("患者信息");
        patient.setFields(Arrays.asList("patientName", "patientGender", "patientAge"));
        orderPhase.getObjects().put(OrderModifyObjectType.PATIENT, patient);

        // 医生信息
        ObjectConfig doctor = new ObjectConfig();
        doctor.setLabel("医生信息");
        doctor.setFields(Arrays.asList("doctorId", "doctorName", "doctorPhone"));
        orderPhase.getObjects().put(OrderModifyObjectType.DOCTOR, doctor);

        // 医院科室
        ObjectConfig hospital = new ObjectConfig();
        hospital.setLabel("医院科室");
        hospital.setFields(Arrays.asList("hospitalId", "hospitalDeptId"));
        orderPhase.getObjects().put(OrderModifyObjectType.HOSPITAL, hospital);

        // 交付信息
        ObjectConfig delivery = new ObjectConfig();
        delivery.setLabel("交付信息");
        delivery.setFields(Arrays.asList("isMailDelivery", "deliveryAddress", "expectedDeliveryTime", "isUrgent"));
        orderPhase.getObjects().put(OrderModifyObjectType.DELIVERY, delivery);

        // 重建项目
        ObjectConfig items = new ObjectConfig();
        items.setLabel("重建项目");
        items.setCoreFields(Arrays.asList("bodyPartId", "projectId"));
        items.setDescFields(Arrays.asList("projectDesc", "moldingRequirement", "otherRequirement"));
        orderPhase.getObjects().put(OrderModifyObjectType.ITEMS, items);

        // 影像文件
        ObjectConfig images = new ObjectConfig();
        images.setLabel("影像文件");
        orderPhase.getObjects().put(OrderModifyObjectType.IMAGES, images);

        config.phases.put("ORDER", orderPhase);

        // DESIGN 阶段配置
        PhaseConfig designPhase = new PhaseConfig();
        designPhase.setAllowedObjects(Arrays.asList(OrderModifyObjectType.ITEMS));
        designPhase.setObjects(orderPhase.getObjects());
        config.phases.put("DESIGN", designPhase);

        return config;
    }
}
