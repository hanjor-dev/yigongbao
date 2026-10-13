package com.yigongbao.module.order;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 测试配置类
 * 排除数据库自动配置，避免测试时初始化数据库
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class TestConfiguration {
}
