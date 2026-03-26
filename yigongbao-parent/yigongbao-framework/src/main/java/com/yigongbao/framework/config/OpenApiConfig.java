package com.yigongbao.boot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 * 访问 /v3/api-docs 获取完整 OpenAPI 3.0 JSON，用于导入 Apifox
 *
 * @author hanjor
 * @date 2026-03-26
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 文档信息（标题、版本、描述）
     * springdoc 会自动扫描所有 Controller 生成接口列表，无需额外配置分组
     *
     * @return OpenAPI
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("医工宝 API 文档")
                        .description("医工宝系统 RESTful API 接口文档，可导入 Apifox 进行接口测试。\n\n" +
                                "**导入方式**：Apifox → 项目设置 → 导入数据 → OpenAPI/Swagger → 粘贴地址 `http://localhost:端口/v3/api-docs`")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("hanjor")
                                .email(""))
                        .license(new License()
                                .name("私有项目")));
    }
}
