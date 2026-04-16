package com.yigongbao.module.design.vo;

import lombok.Data;

/**
 * 生成指令单+图纸结果 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class GenerateDocsResultVO {

    /** 指令单生成结果 */
    private DocItemVO instruction;

    /** 图纸生成结果 */
    private DocItemVO drawing;
}
