package com.yigongbao.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-医院关联 Entity
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("sys_user_hospital")
public class UserHospitalEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long hospitalId;

    private LocalDateTime createTime;
}
