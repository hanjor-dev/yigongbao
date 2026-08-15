package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.ProductionTestConfiguration;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.AssignProductWeightDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ProductionTestConfiguration.class)
@ActiveProfiles("test")
@Import(PrinterSharedAssignmentConcurrencyIntegrationTest.LockProbeConfiguration.class)
class PrinterSharedAssignmentConcurrencyIntegrationTest {

    private static final long FIRST_ADMIN_ID = 9001L;
    private static final long SECOND_ADMIN_ID = 9002L;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DeviceMapper deviceMapper;
    @Autowired private ProductionRecordMapper recordMapper;
    @Autowired private ProductionProcessMapper processMapper;
    @Autowired private ProductionProductMapper productMapper;
    @Autowired private IProductionRecordService recordService;
    @Autowired private DeviceLockProbe lockProbe;

    @BeforeEach
    void prepareDatabase() {
        createFixtureTables();
        jdbcTemplate.update("DELETE FROM production_process");
        jdbcTemplate.update("DELETE FROM production_product");
        jdbcTemplate.update("DELETE FROM production_record");
        jdbcTemplate.update("DELETE FROM device_daily_usage_counter");
        jdbcTemplate.update("DELETE FROM device");
        jdbcTemplate.update("DELETE FROM sys_user");
        insertAdmin(FIRST_ADMIN_ID, "并发管理员甲");
        insertAdmin(SECOND_ADMIN_ID, "并发管理员乙");
    }

    @Test
    void concurrentAssignmentsSerializeOnDeviceAndRequireConfirmationBeforeSharing() throws Exception {
        Fixture fixture = insertFixture();
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch allowFirstToCommit = new CountDownLatch(1);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        lockProbe.reset(firstLocked, allowFirstToCommit, secondAttempted);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> first = null;
        Future<BusinessException> second = null;

        try {
            first = executor.submit(() -> {
                runLoggedIn(FIRST_ADMIN_ID,
                        () -> recordService.assignDevice(fixture.firstRecordId(), assignment(fixture.firstProductId(), false)));
                return null;
            });
            assertTrue(firstLocked.await(5, TimeUnit.SECONDS), "第一事务未及时取得设备锁");

            second = executor.submit(() -> {
                try {
                    runLoggedIn(SECOND_ADMIN_ID,
                            () -> recordService.assignDevice(fixture.secondRecordId(), assignment(fixture.secondProductId(), false)));
                    return null;
                } catch (BusinessException exception) {
                    return exception;
                }
            });
            assertTrue(secondAttempted.await(5, TimeUnit.SECONDS), "第二事务未及时尝试设备锁");
            Future<BusinessException> blocked = second;
            assertThrows(TimeoutException.class, () -> blocked.get(300, TimeUnit.MILLISECONDS),
                    "第二事务应等待第一事务提交");

            allowFirstToCommit.countDown();
            first.get(5, TimeUnit.SECONDS);
            BusinessException exception = second.get(5, TimeUnit.SECONDS);
            assertThat(exception).isNotNull();
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.PRINTER_OCCUPIED_CONFIRM_REQUIRED.getCode());
            assertAssignmentAbsent(fixture.secondRecordId(), fixture.secondProductId());

            runLoggedIn(SECOND_ADMIN_ID, () -> recordService.assignDevice(
                    fixture.secondRecordId(), assignment(fixture.secondProductId(), true)));

            assertAssignmentPersisted(fixture.firstRecordId(), fixture.deviceId());
            assertAssignmentPersisted(fixture.secondRecordId(), fixture.deviceId());
        } finally {
            allowFirstToCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "并发测试线程未及时结束");
            surfaceFailure(first);
            surfaceFailure(second);
            clearLoginContext();
        }
    }

    @Test
    void releasingOneSharedAssignmentLeavesTheOtherRecordProcessAndProductUntouched() {
        Fixture fixture = insertFixture();
        runLoggedIn(FIRST_ADMIN_ID, () -> recordService.assignDevice(
                fixture.firstRecordId(), assignment(fixture.firstProductId(), false)));
        runLoggedIn(SECOND_ADMIN_ID, () -> recordService.assignDevice(
                fixture.secondRecordId(), assignment(fixture.secondProductId(), true)));

        ProductionRecordEntity otherBefore = recordMapper.selectById(fixture.firstRecordId());
        ProductionProcessEntity otherProcessBefore = printProcess(fixture.firstRecordId());
        ProductionProductEntity otherProductBefore = productMapper.selectById(fixture.firstProductId());

        runLoggedIn(SECOND_ADMIN_ID, () -> recordService.releaseDevice(fixture.secondRecordId()));

        ProductionRecordEntity released = recordMapper.selectById(fixture.secondRecordId());
        assertThat(released.getPrintDeviceId()).isNull();
        assertThat(released.getPrintDeviceCode()).isNull();
        assertThat(released.getPrintDeviceName()).isNull();
        assertThat(printProcess(fixture.secondRecordId()).getDeviceId()).isNull();
        assertThat(productMapper.selectById(fixture.secondProductId()).getProductNo()).isNull();
        assertThat(productMapper.selectById(fixture.secondProductId()).getWeight()).isNull();

        ProductionRecordEntity otherAfter = recordMapper.selectById(fixture.firstRecordId());
        ProductionProcessEntity otherProcessAfter = printProcess(fixture.firstRecordId());
        ProductionProductEntity otherProductAfter = productMapper.selectById(fixture.firstProductId());
        assertThat(otherAfter.getPrintDeviceId()).isEqualTo(otherBefore.getPrintDeviceId());
        assertThat(otherAfter.getPrintDeviceCode()).isEqualTo(otherBefore.getPrintDeviceCode());
        assertThat(otherAfter.getPrintDeviceName()).isEqualTo(otherBefore.getPrintDeviceName());
        assertThat(otherAfter.getStatus()).isEqualTo(otherBefore.getStatus());
        assertThat(otherProcessAfter.getDeviceId()).isEqualTo(otherProcessBefore.getDeviceId());
        assertThat(otherProcessAfter.getDeviceNo()).isEqualTo(otherProcessBefore.getDeviceNo());
        assertThat(otherProductAfter.getProductNo()).isEqualTo(otherProductBefore.getProductNo());
        assertThat(otherProductAfter.getWeight()).isEqualByComparingTo(otherProductBefore.getWeight());
    }

    private Fixture insertFixture() {
        DeviceEntity printer = new DeviceEntity();
        printer.setDeviceId("SLA-007");
        printer.setDeviceName("共享并发打印机");
        printer.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
        printer.setState(0);
        printer.setConnectionStatus(1);
        printer.setLastHeartbeat(LocalDateTime.now());
        printer.setIsDeleted(0);
        assertThat(deviceMapper.insert(printer)).isEqualTo(1);

        RecordFixture first = insertRecord("REC-SHARED-001", "260815");
        RecordFixture second = insertRecord("REC-SHARED-002", "260816");
        return new Fixture(printer.getId(), first.recordId(), first.productId(), second.recordId(), second.productId());
    }

    private RecordFixture insertRecord(String recordNo, String batchNo) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setRecordNo(recordNo);
        record.setProductionBatchNo(batchNo);
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        record.setIsDeleted(0);
        assertThat(recordMapper.insert(record)).isEqualTo(1);

        ProductionProductEntity product = new ProductionProductEntity();
        product.setProductionRecordId(record.getId());
        product.setProductName("并发测试产品");
        product.setIsDeleted(0);
        assertThat(productMapper.insert(product)).isEqualTo(1);

        ProductionProcessEntity process = new ProductionProcessEntity();
        process.setProductionRecordId(record.getId());
        process.setProcessType(ProcessTypeEnum.PRINT.getCode());
        process.setProcessName(ProcessTypeEnum.PRINT.getDesc());
        process.setProcessOrder(ProcessTypeEnum.PRINT.getOrder());
        process.setStatus("pending");
        process.setIsDeleted(0);
        assertThat(processMapper.insert(process)).isEqualTo(1);
        return new RecordFixture(record.getId(), product.getId());
    }

    private AssignDeviceDTO assignment(Long productId, boolean confirmOccupied) {
        AssignProductWeightDTO weight = new AssignProductWeightDTO();
        weight.setProductId(productId);
        weight.setWeight(new BigDecimal("1.25"));
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(deviceMapper.selectOne(null).getId());
        dto.setProductWeights(java.util.List.of(weight));
        dto.setConfirmOccupied(confirmOccupied);
        return dto;
    }

    private void assertAssignmentAbsent(Long recordId, Long productId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        assertThat(record.getPrintDeviceId()).isNull();
        assertThat(record.getPrintDeviceCode()).isNull();
        assertThat(record.getPrintDeviceName()).isNull();
        assertThat(printProcess(recordId).getDeviceId()).isNull();
        ProductionProductEntity product = productMapper.selectById(productId);
        assertThat(product.getWeight()).isNull();
        assertThat(product.getProductNo()).isNull();
    }

    private void assertAssignmentPersisted(Long recordId, Long deviceId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        assertThat(record.getPrintDeviceId()).isEqualTo(deviceId);
        assertThat(record.getPrintDeviceCode()).isEqualTo("SLA-007");
        assertThat(record.getPrintDeviceName()).isEqualTo("共享并发打印机");
        assertThat(printProcess(recordId).getDeviceId()).isEqualTo(deviceId);
    }

    private ProductionProcessEntity printProcess(Long recordId) {
        return processMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode()));
    }

    private void runLoggedIn(long userId, Runnable action) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            StpUtil.login(userId);
            action.run();
        } finally {
            clearLoginContext();
        }
    }

    private void insertAdmin(long id, String realName) {
        jdbcTemplate.update("INSERT INTO sys_user (id, real_name, role_code, status, is_deleted) VALUES (?, ?, ?, ?, ?)",
                id, realName, RoleCodeEnum.ADMIN.getCode(), 1, 0);
    }

    private static void clearLoginContext() {
        try {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
        } catch (RuntimeException ignored) {
            // Request context may already be gone during cleanup.
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void createFixtureTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS production_process (
                  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                  production_record_id BIGINT, process_type VARCHAR(100), process_name VARCHAR(255), process_order INT,
                  device_type VARCHAR(100), device_id BIGINT, device_no VARCHAR(100), device_name VARCHAR(255),
                  secondary_device_id BIGINT, secondary_device_no VARCHAR(100), secondary_device_name VARCHAR(255),
                  process_params CLOB, start_time TIMESTAMP, end_time TIMESTAMP, operator_id BIGINT, operator_name VARCHAR(255),
                  has_redo INT, redo_remark VARCHAR(500), inspection_result VARCHAR(100), inspector_id BIGINT,
                  inspector_name VARCHAR(255), status VARCHAR(100), create_time TIMESTAMP, update_time TIMESTAMP,
                  create_by BIGINT, update_by BIGINT, is_deleted INT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user (
                  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, username VARCHAR(100), password VARCHAR(255),
                  real_name VARCHAR(100), phone VARCHAR(50), email VARCHAR(100), sex VARCHAR(20), avatar VARCHAR(500),
                  account_type VARCHAR(50), org_id BIGINT, org_name VARCHAR(255), dept_id BIGINT, dept_name VARCHAR(255),
                  role_id BIGINT, role_name VARCHAR(255), role_code VARCHAR(100), center_id BIGINT, center_name VARCHAR(255),
                  employee_no VARCHAR(100), asset_number VARCHAR(100), specialty VARCHAR(255), qualification VARCHAR(500),
                  settlement_type INT, charging_template_id BIGINT, status INT, login_fail_count INT, lock_time TIMESTAMP,
                  remark VARCHAR(500), order_column_settings CLOB, design_column_settings CLOB,
                  production_column_settings CLOB, quality_column_settings CLOB, warehouse_column_settings CLOB,
                  create_time TIMESTAMP, update_time TIMESTAMP, create_by BIGINT, update_by BIGINT, is_deleted INT
                )
                """);
    }

    private static void surfaceFailure(Future<?> future) throws Exception {
        if (future == null || future.isCancelled() || !future.isDone()) {
            return;
        }
        try {
            future.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }

    private record RecordFixture(Long recordId, Long productId) {}
    private record Fixture(Long deviceId, Long firstRecordId, Long firstProductId,
                           Long secondRecordId, Long secondProductId) {}

    static final class DeviceLockProbe {
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile CountDownLatch firstLocked = new CountDownLatch(0);
        private volatile CountDownLatch allowFirst = new CountDownLatch(0);
        private volatile CountDownLatch secondAttempted = new CountDownLatch(0);

        void reset(CountDownLatch firstLocked, CountDownLatch allowFirst, CountDownLatch secondAttempted) {
            attempts.set(0);
            this.firstLocked = firstLocked;
            this.allowFirst = allowFirst;
            this.secondAttempted = secondAttempted;
        }
    }

    @Aspect
    static final class DeviceLockAspect {
        private final DeviceLockProbe probe;

        DeviceLockAspect(DeviceLockProbe probe) {
            this.probe = probe;
        }

        @Around("bean(deviceMapper) && execution(* selectByIdForUpdate(..))")
        Object observeLock(ProceedingJoinPoint joinPoint) throws Throwable {
            int attempt = probe.attempts.incrementAndGet();
            if (attempt == 2) {
                probe.secondAttempted.countDown();
            }
            Object result = joinPoint.proceed();
            if (attempt == 1) {
                probe.firstLocked.countDown();
                assertTrue(probe.allowFirst.await(5, TimeUnit.SECONDS), "等待放行第一事务超时");
            }
            return result;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class LockProbeConfiguration {
        @Bean DeviceLockProbe deviceLockProbe() {
            return new DeviceLockProbe();
        }

        @Bean DeviceLockAspect deviceLockAspect(DeviceLockProbe probe) {
            return new DeviceLockAspect(probe);
        }
    }
}
