package com.yigongbao.module.system.test.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-14 18:30:00
 */
@Data
public class TestVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 键
     */
    private String key1;

    /**
     * 值
     */
    private String value1;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
