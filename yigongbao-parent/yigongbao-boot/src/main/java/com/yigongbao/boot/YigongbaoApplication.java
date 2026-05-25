package com.yigongbao.boot;

import com.yigongbao.common.config.DefaultConfigProperties;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

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
public class YigongbaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(YigongbaoApplication.class, args);
    }
}
