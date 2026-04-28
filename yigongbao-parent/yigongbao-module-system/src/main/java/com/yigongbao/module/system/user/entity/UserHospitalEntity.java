package com.yigongbao.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-医院关联实体
 * 对应表 sys_user_hospital，hospital_id 实际指向 sys_org.id（医疗机构类型）
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("sys_user_hospital")
public class UserHospitalEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，关联 sys_user.id */
    private Long userId;

    /** 医院ID，关联 sys_org.id（医疗机构类型） */
    private Long hospitalId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
