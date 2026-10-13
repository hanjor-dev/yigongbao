package com.yigongbao.module.system.user.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ManagedOrgSimpleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orgName;
}
