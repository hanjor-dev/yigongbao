package com.yigongbao.module.basic.chargingTemplate.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.chargingTemplate.dto.CreateChargingTemplateDTO;
import com.yigongbao.module.basic.chargingTemplate.dto.UpdateChargingTemplateDTO;
import com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateDetailVO;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateVO;

/**
 * 收费模板 Service
 *
 * @author hanjor
 * @date 2026-06-08
 */
public interface ChargingTemplateService extends IService<ChargingTemplateEntity> {

    /**
     * 分页查询收费模板列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param templateName 模板名称（模糊查询）
     * @return 分页结果
     */
    IPage<ChargingTemplateVO> listPage(Integer pageNum, Integer pageSize, String templateName);

    /**
     * 根据ID查询模板详情（含差异统计）
     *
     * @param id 模板ID
     * @return 模板详情
     */
    ChargingTemplateDetailVO getDetailById(Long id);

    /**
     * 创建收费模板
     *
     * @param dto 创建参数
     * @return 模板ID
     */
    Long create(CreateChargingTemplateDTO dto);

    /**
     * 更新收费模板
     *
     * @param id 模板ID
     * @param dto 更新参数
     */
    void update(Long id, UpdateChargingTemplateDTO dto);

    /**
     * 删除收费模板
     *
     * @param id 模板ID
     */
    void remove(Long id);
}
