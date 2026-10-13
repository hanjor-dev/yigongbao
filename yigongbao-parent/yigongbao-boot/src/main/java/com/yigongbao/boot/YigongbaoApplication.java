package com.yigongbao.boot;

import com.yigongbao.common.config.DefaultConfigProperties;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 医工宝系统启动类
 *
 * @author hanjor
 * @version 1.0
 * @date 2026-03-14
 */
@SpringBootApplication(scanBasePackages = "com.yigongbao")
@MapperScan("com.yigongbao.**.mapper")
@EnableConfigurationProperties(DefaultConfigProperties.class)
@EnableFileStorage
@EnableScheduling
@EnableAsync
public class YigongbaoApplication {

    /** 通用异步执行器，供操作日志等非业务异步任务使用。 */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    public static void main(String[] args) {
        SpringApplication.run(YigongbaoApplication.class, args);
    }
}
