package com.yigongbao.module.system.doctor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.system.doctor.dto.DoctorListDTO;
import com.yigongbao.module.system.doctor.dto.DoctorPageDTO;
import com.yigongbao.module.system.doctor.dto.DoctorSuggestDTO;
import com.yigongbao.module.system.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.system.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import com.yigongbao.module.system.doctor.vo.DoctorVO;

import java.util.List;

/**
 * 医生 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface DoctorService extends IService<DoctorEntity> {

    IPage<DoctorVO> listDoctors(DoctorPageDTO dto);

    List<DoctorVO> listAll(DoctorListDTO dto);

    DoctorVO getById(Long id);

    void create(CreateDoctorDTO dto);

    void update(Long id, UpdateDoctorDTO dto);

    void remove(Long id);

    List<DoctorVO> listByCreatorAndHospital(DoctorSuggestDTO dto);

    /**
     * 快速添加医生
     * 如果医生已存在则返回现有医生，否则创建新医生记录
     */
    DoctorVO quickAdd(QuickAddDoctorDTO dto);

    void updateStatus(Long id, Integer status);
}
