package com.yigongbao.module.design.vo;

import lombok.Data;

/**
 * 打印信息规格 VO
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class PrintInfoSpecVO {

    private Long id;
    private String specName;
    private Long certId;
    private String certNo;
    private String remark;
}
