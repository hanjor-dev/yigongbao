package com.yigongbao.module.order.service.legacy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;

/** 迁移前检查旧库主订单表的必要结构。 */
@Component
public class LegacySourceSchemaValidator {
    private final ObjectProvider<JdbcTemplate> jdbcTemplate;

    public LegacySourceSchemaValidator(@Qualifier("legacyJdbcTemplate") ObjectProvider<JdbcTemplate> jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void validate() {
        try {
            JdbcTemplate legacy = jdbcTemplate.getIfAvailable();
            if (legacy == null) throw new IllegalStateException("旧医工宝数据库未配置或未启用");
            legacy.queryForList("SELECT od_id, od_num FROM jy_order LIMIT 0");
        } catch (Exception ex) {
            throw new IllegalStateException("旧医工宝订单表或必要字段不可用，请确认数据复制已完成。", ex);
        }
    }
}
