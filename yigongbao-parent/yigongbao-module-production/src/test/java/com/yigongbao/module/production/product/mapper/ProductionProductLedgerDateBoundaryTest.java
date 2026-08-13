package com.yigongbao.module.production.product.mapper;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.yigongbao.module.production.record.dto.ProductLedgerExportDTO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionProductLedgerDateBoundaryTest {

    private ProductionProductMapper mapper;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:product-ledger-date-boundary;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema();
        jdbcTemplate.update("DELETE FROM production_process");
        jdbcTemplate.update("DELETE FROM production_product");
        jdbcTemplate.update("DELETE FROM production_record");
        jdbcTemplate.update("DELETE FROM order_main");

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        sqlSessionFactory.getConfiguration().addMapper(ProductionProductMapper.class);
        mapper = sqlSessionFactory.openSession(true).getMapper(ProductionProductMapper.class);
    }

    @Test
    void exclusiveEndTimeIncludesSelectedDateAndExcludesNextDayMidnight() {
        insertOrder();
        insertRecord(1, "2026-08-13 20:00:00");
        insertRecord(2, "2026-08-14 00:00:00");
        insertRecord(3, "2026-08-14 15:00:00");

        ProductLedgerExportDTO dto = new ProductLedgerExportDTO();
        dto.setEndTime(LocalDateTime.of(2026, 8, 14, 0, 0));

        List<Map<String, Object>> rows = mapper.listProductLedgerData(dto);

        assertEquals(1, rows.size());
        assertTrue(rows.getFirst().containsValue("P-1"));
        assertEquals(1L, mapper.countProductLedgerData(dto));
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS order_main (" +
                "id BIGINT PRIMARY KEY, order_code VARCHAR(100), create_time TIMESTAMP, hospital_id BIGINT, " +
                "center_id BIGINT, hospital_name VARCHAR(255), patient_name VARCHAR(255), patient_gender VARCHAR(20), " +
                "patient_age INT, doctor_name VARCHAR(255), hospital_dept_name VARCHAR(255), operator_name VARCHAR(255), " +
                "is_deleted INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS production_record (" +
                "id BIGINT PRIMARY KEY, order_id BIGINT, record_no VARCHAR(100), order_code VARCHAR(100), " +
                "print_start_time TIMESTAMP, print_finish_time TIMESTAMP, producer_name VARCHAR(255), is_deleted INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS production_product (" +
                "id BIGINT PRIMARY KEY, production_record_id BIGINT, product_no VARCHAR(100), file_name VARCHAR(255), " +
                "product_name VARCHAR(255), spec_name VARCHAR(255), color_name VARCHAR(255), material_name VARCHAR(255), " +
                "weight DECIMAL(10,2), warehouse_out_time TIMESTAMP, status VARCHAR(100), is_deleted INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS production_process (" +
                "id BIGINT PRIMARY KEY, production_record_id BIGINT, process_type VARCHAR(100), " +
                "start_time TIMESTAMP, end_time TIMESTAMP, is_deleted INT)");
    }

    private void insertOrder() {
        jdbcTemplate.update("INSERT INTO order_main " +
                        "(id, order_code, create_time, hospital_id, center_id, hospital_name, patient_name, " +
                        "patient_gender, patient_age, doctor_name, hospital_dept_name, operator_name, is_deleted) " +
                        "VALUES (1, 'ORDER-1', TIMESTAMP '2026-08-10 10:00:00', 1, 1, '医院', '患者', " +
                        "'男', 30, '医生', '科室', '业务员', 0)");
    }

    private void insertRecord(long id, String printStartTime) {
        jdbcTemplate.update("INSERT INTO production_record " +
                        "(id, order_id, record_no, order_code, print_start_time, print_finish_time, producer_name, is_deleted) " +
                        "VALUES (?, 1, ?, 'ORDER-1', ?, ?, '生产员', 0)",
                id, "RECORD-" + id, printStartTime, printStartTime);
        jdbcTemplate.update("INSERT INTO production_product " +
                        "(id, production_record_id, product_no, file_name, product_name, spec_name, color_name, " +
                        "material_name, weight, warehouse_out_time, status, is_deleted) " +
                        "VALUES (?, ?, ?, 'part.stl', '产品', '规格', '白色', '树脂', 1.00, NULL, 'in_process', 0)",
                id, id, "P-" + id);
    }
}
