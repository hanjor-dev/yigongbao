package com.yigongbao.module.basic.registrationCert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * RegistrationCert 模块测试启动类
 * 仅用于 registrationCert 的 SpringBootTest 场景
 *
 * @author hanjor
 * @date 2026-03-24
 */
@SpringBootApplication(scanBasePackages = "com.yigongbao")
@MapperScan("com.yigongbao.**.mapper")
@ComponentScan(
    basePackages = "com.yigongbao",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.yigongbao\\.module\\.basic\\.registrationCert\\.test\\..*"
    )
)
public class RegistrationCertTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistrationCertTestApplication.class, args);
    }
}
