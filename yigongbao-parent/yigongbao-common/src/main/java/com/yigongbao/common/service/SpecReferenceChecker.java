package com.yigongbao.common.service;

/**
 * 产品规格引用检查接口
 * 由 design 模块实现，basic 模块在删除规格前调用，防止循环依赖
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface SpecReferenceChecker {

    /**
     * 检查规格是否被打印信息（design_product）引用
     *
     * @param specId 规格ID
     * @return true 表示存在引用（is_deleted=0 的记录）
     */
    boolean isSpecInUse(Long specId);
}
