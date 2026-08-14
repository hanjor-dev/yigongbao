package com.yigongbao.module.production.device;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.production.ProductionTestConfiguration;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(PrinterDeviceStateConcurrencyIntegrationTest.LockAttemptTestConfiguration.class)
class PrinterDeviceStateConcurrencyIntegrationTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ProductionRecordMapper recordMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DeviceLockAttemptProbe lockAttemptProbe;

    @BeforeEach
    void cleanDatabase() {
        recordMapper.delete(null);
        deviceMapper.delete(null);
    }

    @Test
    void manualStateChangeWaitsForDeviceLockThenRejectsNewlyCommittedActiveBinding() throws Exception {
        DeviceEntity printer = new DeviceEntity();
        printer.setDeviceId("SLA-CONCURRENT-001");
        printer.setDeviceName("并发测试打印机");
        printer.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
        printer.setState(0);
        printer.setConnectionStatus(1);
        printer.setLastHeartbeat(LocalDateTime.now());
        printer.setIsDeleted(0);
        assertThat(deviceMapper.insert(printer)).isEqualTo(1);

        CountDownLatch deviceLocked = new CountDownLatch(1);
        CountDownLatch allowBind = new CountDownLatch(1);
        CountDownLatch lockAttempted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Void> bindFuture = null;
        Future<BusinessException> stateChangeFuture = null;

        try {
            lockAttemptProbe.reset(lockAttempted);

            bindFuture = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    DeviceEntity locked = deviceMapper.selectByIdForUpdate(printer.getId());
                    assertThat(locked).isNotNull();
                    deviceLocked.countDown();
                    await(allowBind);

                    ProductionRecordEntity record = new ProductionRecordEntity();
                    record.setRecordNo("REC-CONCURRENT-001");
                    record.setPrintDeviceId(printer.getId());
                    record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
                    record.setIsDeleted(0);
                    assertThat(recordMapper.insert(record)).isEqualTo(1);
                });
                return null;
            });

            assertTrue(deviceLocked.await(5, TimeUnit.SECONDS), "事务 A 未及时取得设备行锁");

            stateChangeFuture = executor.submit(() -> {
                try {
                    deviceService.updateDeviceState(printer.getId(), 6);
                    return null;
                } catch (BusinessException exception) {
                    return exception;
                }
            });

            assertTrue(lockAttempted.await(5, TimeUnit.SECONDS),
                    "事务 B 未及时进入第二次设备行锁查询");
            Future<BusinessException> blockedStateChange = stateChangeFuture;
            assertThrows(TimeoutException.class,
                    () -> blockedStateChange.get(300, TimeUnit.MILLISECONDS),
                    "事务 B 应等待事务 A 持有的设备行锁");

            allowBind.countDown();
            bindFuture.get(5, TimeUnit.SECONDS);
            BusinessException exception = stateChangeFuture.get(5, TimeUnit.SECONDS);

            assertThat(exception).isNotNull();
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
            assertThat(deviceMapper.selectById(printer.getId()).getState()).isZero();
        } finally {
            allowBind.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "并发测试线程未及时结束");
            surfaceFailure(bindFuture);
            surfaceFailure(stateChangeFuture);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "等待并发测试信号超时");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("等待并发测试信号被中断", exception);
        }
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

    static final class DeviceLockAttemptProbe {
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile CountDownLatch secondAttempt = new CountDownLatch(0);

        void reset(CountDownLatch secondAttempt) {
            attempts.set(0);
            this.secondAttempt = secondAttempt;
        }

        void beforeLockAttempt() {
            if (attempts.incrementAndGet() == 2) {
                secondAttempt.countDown();
            }
        }
    }

    @Aspect
    static final class DeviceMapperLockAttemptAspect {
        private final DeviceLockAttemptProbe probe;

        DeviceMapperLockAttemptAspect(DeviceLockAttemptProbe probe) {
            this.probe = probe;
        }

        @Around("bean(deviceMapper) && execution(* selectByIdForUpdate(..))")
        Object observeLockAttempt(ProceedingJoinPoint joinPoint) throws Throwable {
            probe.beforeLockAttempt();
            return joinPoint.proceed();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class LockAttemptTestConfiguration {

        @Bean
        DeviceLockAttemptProbe deviceLockAttemptProbe() {
            return new DeviceLockAttemptProbe();
        }

        @Bean
        DeviceMapperLockAttemptAspect deviceMapperLockAttemptAspect(DeviceLockAttemptProbe probe) {
            return new DeviceMapperLockAttemptAspect(probe);
        }
    }
}
