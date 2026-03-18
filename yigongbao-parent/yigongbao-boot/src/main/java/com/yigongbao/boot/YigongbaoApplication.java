package com.yigongbao.boot;

import com.yigongbao.common.config.DefaultConfigProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
public class YigongbaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(YigongbaoApplication.class, args);
    }
}
