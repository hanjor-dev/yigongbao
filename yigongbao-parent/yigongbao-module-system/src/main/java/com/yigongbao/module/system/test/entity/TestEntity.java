package com.yigongbao.module.system.test.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 测试 Entity
 *
 * @author hanjor
 * @date 2026-03-14 18:25:00
 */
@Data
@TableName("test")
@EqualsAndHashCode(callSuper = true)
public class TestEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 键
     */
    private String key1;

    /**
     * 值
     */
    private String value1;
}
