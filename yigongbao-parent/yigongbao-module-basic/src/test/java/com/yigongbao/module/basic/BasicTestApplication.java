package com.yigongbao.module.basic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Basic 模块测试启动类
 * 仅用于 module-basic 的 SpringBootTest 场景
 *
 * @author hanjor
 * @date 2026-03-19
 */
@SpringBootApplication(scanBasePackages = "com.yigongbao")
@MapperScan("com.yigongbao.module.basic.**.mapper")
public class BasicTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicTestApplication.class, args);
    }
}
