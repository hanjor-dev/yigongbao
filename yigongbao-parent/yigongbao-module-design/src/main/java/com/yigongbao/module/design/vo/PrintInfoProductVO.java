package com.yigongbao.module.design.vo;

import lombok.Data;

import java.util.List;

/**
 * 打印信息产品 VO（产品树节点，含 specs）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class PrintInfoProductVO {

    private Long id;
    private String productName;
    /** 产品大类 dict_code（如 17.1） */
    private String category;
    private String categoryName;
    private List<PrintInfoSpecVO> specs;
}
