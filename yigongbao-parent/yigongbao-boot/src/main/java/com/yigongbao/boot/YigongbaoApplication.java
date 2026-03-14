package com.yigongbao.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 医工宝系统启动类
 *
 * @author hanjor
 * @version 1.0
 * @date 2026-03-14
 */
@SpringBootApplication(scanBasePackages = "com.yigongbao")
@MapperScan("com.yigongbao.**.mapper")
public class YigongbaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(YigongbaoApplication.class, args);
    }
}
