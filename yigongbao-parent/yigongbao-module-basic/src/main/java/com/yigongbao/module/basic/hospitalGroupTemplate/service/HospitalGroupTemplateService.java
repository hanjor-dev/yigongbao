package com.yigongbao.module.basic.hospitalGroupTemplate.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.HospitalGroupTemplatePageDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;

import java.util.List;

/**
 * 医院组合模板 Service 接口
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface HospitalGroupTemplateService extends IService<HospitalGroupTemplateEntity> {

    /**
     * 分页查询模板列表
     */
    IPage<HospitalGroupTemplateVO> listTemplate(HospitalGroupTemplatePageDTO dto);

    /**
     * 根据ID查询模板详情（含明细）
     */
    HospitalGroupTemplateVO getTemplateById(Long id);

    /**
     * 创建模板
     */
    void createTemplate(CreateHospitalGroupTemplateDTO dto);

    /**
     * 更新模板
     */
    void updateTemplate(Long id, UpdateHospitalGroupTemplateDTO dto);

    /**
     * 删除模板
     */
    void removeTemplate(Long id);

    /**
     * 修改模板状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取模板下拉选项
     */
    List<HospitalGroupTemplateSimpleVO> listOptions(Integer status);
}
