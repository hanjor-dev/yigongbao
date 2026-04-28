package com.yigongbao.module.system.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分配用户医院范围 DTO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class AssignHospitalsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 要分配的医院ID列表，不能为空 */
    @NotEmpty(message = "医院列表不能为空")
    private List<Long> hospitalIds;
}
