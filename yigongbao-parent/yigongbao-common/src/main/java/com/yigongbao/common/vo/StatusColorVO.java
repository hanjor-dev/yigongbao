package com.yigongbao.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 状态标签颜色配置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusColorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签背景色 */
    private String bgColor;

    /** 标签边框色 */
    private String bdColor;

    /** 标签文字色 */
    private String color;
}
