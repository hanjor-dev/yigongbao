package com.yigongbao.module.system.hospitalGroupTemplate.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.HospitalGroupTemplatePageDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;

import java.util.List;

/**
 * 医院组合模板 Service 接口
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface HospitalGroupTemplateService extends IService<HospitalGroupTemplateEntity> {

    IPage<HospitalGroupTemplateVO> listTemplate(HospitalGroupTemplatePageDTO dto);

    /**
     * 根据ID查询模板详情
     *
     * @param id     模板ID
     * @param userId 用户ID（可选，传入时 assigned 字段表示该用户是否已分配；不传时表示全系统任意用户是否已分配）
     * @return 模板详情VO
     */
    HospitalGroupTemplateVO getTemplateById(Long id, Long userId);

    void createTemplate(CreateHospitalGroupTemplateDTO dto);

    void updateTemplate(Long id, UpdateHospitalGroupTemplateDTO dto);

    void removeTemplate(Long id);

    void updateStatus(Long id, Integer status);

    List<HospitalGroupTemplateSimpleVO> listOptions();
}
