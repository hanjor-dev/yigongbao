package com.yigongbao.module.production.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseDTO;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseProductDTO;
import com.yigongbao.module.production.warehouse.vo.WarehouseProductVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 生产流转卡 Mapper
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Mapper
public interface ProductionRecordMapper extends BaseMapper<ProductionRecordEntity> {

    /** 按主键锁定流转卡，串行化设备分配与释放操作。 */
    @Select("SELECT * FROM production_record WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    ProductionRecordEntity selectByIdForUpdate(@Param("id") Long id);

    @Select("<script>" +
            "SELECT r.id AS recordId, r.record_no AS recordNo, r.order_code AS orderNo, " +
            "r.hospital_name AS hospitalName, r.hospital_dept_name AS hospitalDeptName, " +
            "r.doctor_name AS doctorName, r.patient_name AS patientName, " +
            "r.is_urgent AS isUrgent, r.is_postal AS isPostal, r.expected_delivery_date AS expectedDeliveryDate, " +
            "r.processing_center_name AS processingCenterName, r.design_package_code AS designPackageCode, " +
            "r.production_batch_no AS productionBatchNo, r.material_batch_no AS materialBatchNo, r.status, " +
            "COUNT(p.id) AS totalCount, " +
            "SUM(CASE WHEN p.status = 'pending_warehouse_in' THEN 1 ELSE 0 END) AS pendingWarehouseInCount, " +
            "SUM(CASE WHEN p.status = 'warehoused' THEN 1 ELSE 0 END) AS warehousedCount, " +
            "SUM(CASE WHEN p.status = 'warehouse_out' THEN 1 ELSE 0 END) AS warehouseOutCount, " +
            "MIN(p.warehouse_in_time) AS earliestInTime, MAX(p.warehouse_out_time) AS latestOutTime " +
            "FROM production_record r " +
            "INNER JOIN production_product p ON r.id = p.production_record_id AND p.is_deleted = 0 " +
            "WHERE r.is_deleted = 0 " +
            "<if test='dto.keyword != null and dto.keyword != \"\"'>" +
            "AND (r.record_no LIKE CONCAT('%', #{dto.keyword}, '%') " +
            "OR r.order_code LIKE CONCAT('%', #{dto.keyword}, '%') " +
            "OR p.product_name LIKE CONCAT('%', #{dto.keyword}, '%') " +
            "OR p.product_no LIKE CONCAT('%', #{dto.keyword}, '%')) " +
            "</if>" +
            "<if test='dto.status != null'>AND r.status = #{dto.status}</if>" +
            "<if test='dto.warehouseInTimeStart != null'>AND p.warehouse_in_time &gt;= #{dto.warehouseInTimeStart}</if>" +
            "<if test='dto.warehouseInTimeEnd != null'>AND p.warehouse_in_time &lt;= #{dto.warehouseInTimeEnd}</if>" +
            "<if test='dto.warehouseOutTimeStart != null'>AND p.warehouse_out_time &gt;= #{dto.warehouseOutTimeStart}</if>" +
            "<if test='dto.warehouseOutTimeEnd != null'>AND p.warehouse_out_time &lt;= #{dto.warehouseOutTimeEnd}</if>" +
            "GROUP BY r.id, r.record_no, r.order_code, r.hospital_name, r.hospital_dept_name, " +
            "r.doctor_name, r.patient_name, r.is_urgent, r.is_postal, r.expected_delivery_date, " +
            "r.processing_center_name, r.design_package_code, r.production_batch_no, r.material_batch_no, r.status " +
            "ORDER BY r.create_time DESC" +
            "</script>")
    IPage<WarehouseRecordVO> listWarehouse(Page<WarehouseRecordVO> page, @Param("dto") ListWarehouseDTO dto);

    @Select("<script>" +
            "SELECT p.id AS productId, p.product_no AS productNo, p.product_name AS productName, " +
            "p.spec_name AS specName, p.material_name AS materialName, p.color_name AS colorName, p.status, " +
            "r.record_no AS recordNo, r.order_code AS orderNo, r.hospital_name AS hospitalName, r.patient_name AS patientName, " +
            "p.warehouse_in_time AS warehouseInTime, p.warehouse_in_remark AS warehouseInRemark, " +
            "p.warehouse_out_time AS warehouseOutTime, p.warehouse_out_remark AS warehouseOutRemark " +
            "FROM production_product p " +
            "INNER JOIN production_record r ON p.production_record_id = r.id AND r.is_deleted = 0 " +
            "WHERE p.is_deleted = 0 " +
            "<if test='dto.keyword != null and dto.keyword != \"\"'>" +
            "AND (p.product_no LIKE CONCAT('%', #{dto.keyword}, '%') " +
            "OR p.product_name LIKE CONCAT('%', #{dto.keyword}, '%') " +
            "OR r.record_no LIKE CONCAT('%', #{dto.keyword}, '%') " +
            "OR r.order_code LIKE CONCAT('%', #{dto.keyword}, '%')) " +
            "</if>" +
            "<if test='dto.status != null and dto.status != \"\"'>AND p.status = #{dto.status}</if>" +
            "<if test='dto.warehouseInTimeStart != null'>AND p.warehouse_in_time &gt;= #{dto.warehouseInTimeStart}</if>" +
            "<if test='dto.warehouseInTimeEnd != null'>AND p.warehouse_in_time &lt;= #{dto.warehouseInTimeEnd}</if>" +
            "<if test='dto.warehouseOutTimeStart != null'>AND p.warehouse_out_time &gt;= #{dto.warehouseOutTimeStart}</if>" +
            "<if test='dto.warehouseOutTimeEnd != null'>AND p.warehouse_out_time &lt;= #{dto.warehouseOutTimeEnd}</if>" +
            "ORDER BY p.create_time DESC" +
            "</script>")
    IPage<WarehouseProductVO> listWarehouseProducts(Page<WarehouseProductVO> page, @Param("dto") ListWarehouseProductDTO dto);
}
