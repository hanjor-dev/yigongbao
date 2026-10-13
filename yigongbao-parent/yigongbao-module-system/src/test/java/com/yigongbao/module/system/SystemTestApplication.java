package com.yigongbao.module.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * System 模块测试启动类
 * 仅用于 module-system 的 SpringBootTest 场景
 * 排除 test 包，避免 TestController/TestService 的 Mapper 依赖问题
 *
 * @author hanjor
 * @date 2026-03-16
 */
@SpringBootApplication(scanBasePackages = "com.yigongbao")
@MapperScan("com.yigongbao.**.mapper")
@ComponentScan(
    basePackages = "com.yigongbao",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.yigongbao\\.module\\.system\\.test\\..*|com\\.yigongbao\\.module\\.basic\\.test\\..*"
    )
)
public class SystemTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemTestApplication.class, args);
    }
}
