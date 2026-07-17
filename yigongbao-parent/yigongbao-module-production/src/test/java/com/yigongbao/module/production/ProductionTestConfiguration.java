package com.yigongbao.module.production;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.Mockito.mock;

/**
 * 生产模块测试配置类
 * 用于集成测试的Spring Boot配置
 *
 * @author hanjor
 * @date 2026-07-13
 */
@SpringBootApplication(scanBasePackages = {
    "com.yigongbao.module.production",
    "com.yigongbao.module.design",
    "com.yigongbao.module.order",
    "com.yigongbao.module.basic",
    "com.yigongbao.module.system",
    "com.yigongbao.module.flow",
    "com.yigongbao.common",
    "com.yigongbao.framework"
})
@MapperScan({
    "com.yigongbao.module.production.**.mapper",
    "com.yigongbao.module.design.**.mapper",
    "com.yigongbao.module.order.mapper",
    "com.yigongbao.module.basic.**.mapper",
    "com.yigongbao.module.system.**.mapper"
})
public class ProductionTestConfiguration {

    @Bean
    public FlowFacade flowFacade() {
        return mock(FlowFacade.class);
    }

    @Bean
    public FlowOrderService flowOrderService() {
        return mock(FlowOrderService.class);
    }

    @Bean
    public FlowStatusHistoryService flowStatusHistoryService() {
        return mock(FlowStatusHistoryService.class);
    }

    @Bean
    public FileStorageService fileStorageService() {
        return mock(FileStorageService.class);
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
