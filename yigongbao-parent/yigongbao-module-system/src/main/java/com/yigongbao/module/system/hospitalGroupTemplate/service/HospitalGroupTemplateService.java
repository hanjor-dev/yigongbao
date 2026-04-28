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

    HospitalGroupTemplateVO getTemplateById(Long id);

    void createTemplate(CreateHospitalGroupTemplateDTO dto);

    void updateTemplate(Long id, UpdateHospitalGroupTemplateDTO dto);

    void removeTemplate(Long id);

    void updateStatus(Long id, Integer status);

    List<HospitalGroupTemplateSimpleVO> listOptions(Integer status);
}
