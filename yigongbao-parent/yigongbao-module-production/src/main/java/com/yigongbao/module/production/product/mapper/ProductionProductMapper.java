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
     * 查询维度：产品级别（非订单级别、非流转卡级别），包含台账导出所需的产品、订单和生产时长信息。
     * 数据权限：通过 dto.hospitalIds 或 dto.centerIds 自动过滤（由Service层填充）。
     * 性能优化：使用标量聚合子查询计算后处理时长，避免多表JOIN导致产品行重复。
     * </p>
     *
     * @param dto 查询条件（包含recordNo/orderCode/productNo/时间范围/权限过滤ID）
     * @return 产品台账数据列表（Map格式，包含所有导出字段）
     */
    @Select("<script>" +
            "SELECT " +
            "    om.order_code AS order_code, " +
            "    om.create_time AS order_create_time, " +
            "    pp.product_no AS product_no, " +
            "    pp.file_name AS file_name, " +
            "    pp.product_name AS product_name, " +
            "    pp.spec_name AS spec_name, " +
            "    pp.color_name AS color_name, " +
            "    pp.material_name AS material_name, " +
            "    CASE WHEN pr.print_start_time IS NOT NULL AND pr.print_finish_time IS NOT NULL " +
            "              AND pr.print_finish_time &gt;= pr.print_start_time " +
            "         THEN TIMESTAMPDIFF(SECOND, pr.print_start_time, pr.print_finish_time) " +
            "         ELSE NULL END AS print_duration_seconds, " +
            "    pp.weight AS weight, " +
            "    (SELECT CASE " +
            "                WHEN COUNT(*) = 0 THEN 0 " +
            "                WHEN SUM(CASE WHEN p_process.start_time IS NULL OR p_process.end_time IS NULL " +
            "                                   OR p_process.end_time &lt; p_process.start_time " +
            "                              THEN 1 ELSE 0 END) &gt; 0 THEN NULL " +
            "                ELSE SUM(TIMESTAMPDIFF(SECOND, p_process.start_time, p_process.end_time)) " +
            "            END " +
            "       FROM production_process p_process " +
            "      WHERE p_process.production_record_id = pr.id " +
            "        AND p_process.process_type IN ('wash', 'cure', 'clean_dry') " +
            "        AND p_process.is_deleted = 0) AS processing_duration_seconds, " +
            "    om.hospital_name AS hospital_name, " +
            "    om.patient_name AS patient_name, " +
            "    om.patient_gender AS patient_gender, " +
            "    om.patient_age AS patient_age, " +
            "    pr.producer_name AS producer_name, " +
            "    om.doctor_name AS doctor_name, " +
            "    om.hospital_dept_name AS hospital_dept_name, " +
            "    om.operator_name AS business_operator, " +
            "    pp.warehouse_out_time AS warehouse_out_time " +
            "FROM production_product pp " +
            "INNER JOIN production_record pr ON pp.production_record_id = pr.id " +
            "INNER JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0 " +
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
            "  AND pr.print_start_time &gt;= #{dto.startTime} " +
            "</if>" +
            "<if test='dto.endTime != null'>" +
            "  AND pr.print_start_time &lt; #{dto.endTime} " +
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
            "ORDER BY om.create_time DESC, pp.id DESC " +
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
            "INNER JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0 " +
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
            "  AND pr.print_start_time &gt;= #{dto.startTime} " +
            "</if>" +
            "<if test='dto.endTime != null'>" +
            "  AND pr.print_start_time &lt; #{dto.endTime} " +
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
