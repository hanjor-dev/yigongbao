package com.yigongbao.module.design.service;

import com.yigongbao.module.design.dto.SavePrintInfoDTO;
import com.yigongbao.module.design.vo.DesignProductVO;
import com.yigongbao.module.design.vo.PrintInfoOptionsVO;

import java.util.List;

/**
 * 打印信息管理 Service 接口
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignPrintInfoService {

    /**
     * 获取打印信息选项数据（产品树、材质、颜色）以及包级已保存回显字段
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 选项 VO
     */
    PrintInfoOptionsVO getOptions(Long orderId, Long packageId);

    /**
     * 查询数据包打印信息列表
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 打印信息列表，按 sort_order 升序
     */
    List<DesignProductVO> listPrintInfo(Long orderId, Long packageId);

    /**
     * 保存打印信息（整包替换）
     * 空列表表示清空该数据包的打印信息
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param dto       请求 DTO
     */
    void savePrintInfo(Long orderId, Long packageId, SavePrintInfoDTO dto);

    /**
     * 删除单条打印信息
     *
     * @param orderId      订单ID
     * @param packageId    数据包ID
     * @param printInfoId  打印信息ID
     */
    void deletePrintInfo(Long orderId, Long packageId, Long printInfoId);
}
