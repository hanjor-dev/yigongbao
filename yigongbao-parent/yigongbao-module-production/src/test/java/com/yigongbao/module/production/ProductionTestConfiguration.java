package com.yigongbao.module.production;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
}
