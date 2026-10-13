package com.yigongbao.framework.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 * 配置分页插件、逻辑删除等常用功能
 *
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 插件集合
     * 目前配置了分页插件和逻辑删除插件
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 配置自动填充处理器
     * 在插入或更新记录时，自动填充公共字段
     * - createTime: 创建时间
     * - updateTime: 更新时间
     * - createBy: 创建人ID（从Sa-Token获取，未登录则为null）
     * - updateBy: 更新人ID（从Sa-Token获取，未登录则为null）
     * - isDeleted: 是否删除（默认为0）
     *
     * @return MetaObjectHandler 实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入时自动填充
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // 填充创建时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                // 填充更新时间
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                // 填充创建人，未登录则为空
                this.strictInsertFill(metaObject, "createBy", Long.class, getUserId());
                // 填充更新人，未登录则为空
                this.strictInsertFill(metaObject, "updateBy", Long.class, getUserId());
                // 填充是否删除，默认0
                this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);

            }

            /**
             * 更新时自动填充
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // 填充更新时间（使用 fillStrategy 强制更新，即使字段已有值）
                this.fillStrategy(metaObject, "updateTime", LocalDateTime.now());
                // 填充更新人（使用 fillStrategy 强制更新）
                this.fillStrategy(metaObject, "updateBy", getUserId());
            }

            /**
             * 获取当前登录用户ID，未登录返回null
             * 注意：此处获取的是 Sa-Token 的 loginId，需要确保登录时存入的是用户ID
             */
            private Long getUserId() {
                try {
                    if (StpUtil.isLogin()) {
                        return StpUtil.getLoginIdAsLong();
                    }
                } catch (Exception ignored) {
                }
                return null;
            }
        };
    }

}
