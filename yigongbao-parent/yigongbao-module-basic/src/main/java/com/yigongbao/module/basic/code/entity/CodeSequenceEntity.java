package com.yigongbao.module.basic.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 编码序号 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("sys_code_sequence")
public class CodeSequenceEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 业务标识（用于按业务维度隔离序号，如订单编号）
     * 为空时表示全局序号
     */
    private String bizKey;

    /**
     * 当前序号
     */
    private Long currentSeq;

    /**
     * 上次重置日期（用于判断是否需要重置）
     */
    private LocalDate lastDate;

    /**
     * 乐观锁版本号（配合 @Version 注解自动处理并发）
     */
    @Version
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
