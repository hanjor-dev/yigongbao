package com.yigongbao.module.production.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.record.dto.ProductLedgerExportDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 生产产品 Mapper
 * <p>
 * 负责生产产品数据的持久层操作，包括基础CRUD和台账导出查询。
 * </p>
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Mapper
public interface ProductionProductMapper extends BaseMapper<ProductionProductEntity> {

    /**
     * 查询生产产品台账数据（用于Excel导出）
     * <p>
     * 查询维度：产品级别（非订单级别、非流转卡级别），包含产品信息、订单信息、流转卡信息、工序人员、质检仓储信息。
     * 数据权限：通过 dto.hospitalIds 或 dto.centerIds 自动过滤（由Service层填充）。
     * 性能优化：使用子查询获取各工序操作员，避免多表JOIN导致笛卡尔积。
     * </p>
     *
     * @param dto 查询条件（包含recordNo/orderCode/productNo/时间范围/权限过滤ID）
     * @return 产品台账数据列表（Map格式，包含所有导出字段）
     */
    @Select("<script>" +
            "SELECT " +
            "    pp.product_no, pp.product_name, pp.spec_name, pp.material_name, pp.color_name, " +
            "    pp.cert_no, pp.file_name, pp.udi_code, pp.status AS product_status, " +
            "    pr.current_process AS current_process_type, " +
            "    pp.qc_result, pp.qc_time, pp.qc_remark, pp.warehouse_in_time, pp.warehouse_out_time, pp.create_time, " +
            "    pr.order_code, pr.order_type, pr.hospital_name, pr.hospital_dept_name, pr.doctor_name, pr.patient_name, " +
            "    pr.is_urgent, pr.is_postal, pr.expected_delivery_date, " +
            "    pr.record_no, pr.production_batch_no, pr.design_package_code, " +
            "    COALESCE(pc.center_name, om.center_name) AS processing_center_name, " +
            "    pr.print_device_code, pr.print_start_time, pr.print_finish_time, " +
            "    pr.material_batch_no, pr.pack_operator_name, pr.pack_time, " +
            "    u_qc.real_name AS qc_user_name, " +
            "    u_in.real_name AS warehouse_in_user_name, " +
            "    u_out.real_name AS warehouse_out_user_name, " +
            "    (SELECT operator_name FROM production_process WHERE production_record_id = pr.id AND process_type = 'print' AND is_deleted = 0 LIMIT 1) AS print_operator, " +
            "    (SELECT operator_name FROM production_process WHERE production_record_id = pr.id AND process_type = 'wash' AND is_deleted = 0 LIMIT 1) AS wash_operator, " +
            "    (SELECT operator_name FROM production_process WHERE production_record_id = pr.id AND process_type = 'cure' AND is_deleted = 0 LIMIT 1) AS cure_operator " +
            "FROM production_product pp " +
            "INNER JOIN production_record pr ON pp.production_record_id = pr.id " +
            "LEFT JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0 " +
            "LEFT JOIN processing_center pc ON om.center_id = pc.id AND pc.is_deleted = 0 " +
            "LEFT JOIN sys_user u_qc ON pp.qc_user_id = u_qc.id " +
            "LEFT JOIN sys_user u_in ON pp.warehouse_in_user_id = u_in.id " +
            "LEFT JOIN sys_user u_out ON pp.warehouse_out_user_id = u_out.id " +
            "WHERE pp.is_deleted = 0 AND pr.is_deleted = 0 " +
            "  AND pp.status IN ('in_process', 'fail', 'pass', 'pending_warehouse_in', 'warehoused', 'warehouse_out', 'completed', 'cancelled') " +
            "<if test='dto.recordNo != null and dto.recordNo != \"\"'>" +
            "  AND pr.record_no LIKE CONCAT('%', #{dto.recordNo}, '%') " +
            "</if>" +
            "<if test='dto.orderCode != null and dto.orderCode != \"\"'>" +
            "  AND pr.order_code LIKE CONCAT('%', #{dto.orderCode}, '%') " +
            "</if>" +
            "<if test='dto.productNo != null and dto.productNo != \"\"'>" +
            "  AND pp.product_no LIKE CONCAT('%', #{dto.productNo}, '%') " +
            "</if>" +
            "<if test='dto.startTime != null'>" +
            "  AND pp.create_time &gt;= #{dto.startTime} " +
            "</if>" +
            "<if test='dto.endTime != null'>" +
            "  AND pp.create_time &lt;= #{dto.endTime} " +
            "</if>" +
            "<if test='dto.hospitalIds != null and dto.hospitalIds.size() > 0'>" +
            "  AND om.hospital_id IN " +
            "  <foreach collection='dto.hospitalIds' item='hospitalId' open='(' separator=',' close=')'>" +
            "    #{hospitalId}" +
            "  </foreach>" +
            "</if>" +
            "<if test='dto.centerIds != null and dto.centerIds.size() > 0'>" +
            "  AND om.center_id IN " +
            "  <foreach collection='dto.centerIds' item='centerId' open='(' separator=',' close=')'>" +
            "    #{centerId}" +
            "  </foreach>" +
            "</if>" +
            "ORDER BY pp.create_time DESC " +
            "LIMIT 10000" +
            "</script>")
    List<Map<String, Object>> listProductLedgerData(@Param("dto") ProductLedgerExportDTO dto);

    /**
     * 统计生产产品台账数据总数（用于判断是否超过1万条）
     * <p>
     * 查询条件与 listProductLedgerData 完全一致，用于：
     * 1. 判断查询结果是否为空
     * 2. 判断是否超过1万条限制，超过时在Excel顶部显示红色警告
     * </p>
     *
     * @param dto 查询条件（同 listProductLedgerData）
     * @return 符合条件的产品总数
     */
    @Select("<script>" +
            "SELECT COUNT(1) " +
            "FROM production_product pp " +
            "INNER JOIN production_record pr ON pp.production_record_id = pr.id " +
            "LEFT JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0 " +
            "WHERE pp.is_deleted = 0 AND pr.is_deleted = 0 " +
            "  AND pp.status IN ('in_process', 'fail', 'pass', 'pending_warehouse_in', 'warehoused', 'warehouse_out', 'completed', 'cancelled') " +
            "<if test='dto.recordNo != null and dto.recordNo != \"\"'>" +
            "  AND pr.record_no LIKE CONCAT('%', #{dto.recordNo}, '%') " +
            "</if>" +
            "<if test='dto.orderCode != null and dto.orderCode != \"\"'>" +
            "  AND pr.order_code LIKE CONCAT('%', #{dto.orderCode}, '%') " +
            "</if>" +
            "<if test='dto.productNo != null and dto.productNo != \"\"'>" +
            "  AND pp.product_no LIKE CONCAT('%', #{dto.productNo}, '%') " +
            "</if>" +
            "<if test='dto.startTime != null'>" +
            "  AND pp.create_time &gt;= #{dto.startTime} " +
            "</if>" +
            "<if test='dto.endTime != null'>" +
            "  AND pp.create_time &lt;= #{dto.endTime} " +
            "</if>" +
            "<if test='dto.hospitalIds != null and dto.hospitalIds.size() > 0'>" +
            "  AND om.hospital_id IN " +
            "  <foreach collection='dto.hospitalIds' item='hospitalId' open='(' separator=',' close=')'>" +
            "    #{hospitalId}" +
            "  </foreach>" +
            "</if>" +
            "<if test='dto.centerIds != null and dto.centerIds.size() > 0'>" +
            "  AND om.center_id IN " +
            "  <foreach collection='dto.centerIds' item='centerId' open='(' separator=',' close=')'>" +
            "    #{centerId}" +
            "  </foreach>" +
            "</if>" +
            "</script>")
    Long countProductLedgerData(@Param("dto") ProductLedgerExportDTO dto);
}
