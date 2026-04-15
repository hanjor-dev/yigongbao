package com.yigongbao.module.design.vo;

import lombok.Data;

/**
 * 字典选项 VO（材质/颜色通用）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DictOptionVO {

    /** dict_code，如 15.1、16.1.1 */
    private String code;

    /** dict_name，如 树脂、白色 */
    private String name;

    /** 是否默认（仅 materials 使用，15.1=树脂 为默认） */
    private Boolean isDefault;
}
