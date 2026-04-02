package com.yigongbao.module.basic.doctor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.DoctorListDTO;
import com.yigongbao.module.basic.doctor.dto.DoctorPageDTO;
import com.yigongbao.module.basic.doctor.dto.DoctorSuggestDTO;
import com.yigongbao.module.basic.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.basic.doctor.entity.DoctorEntity;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;

import java.util.List;

/**
 * 医生 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface DoctorService extends IService<DoctorEntity> {

    /**
     * 分页查询医生列表
     */
    IPage<DoctorVO> listDoctors(DoctorPageDTO dto);

    /**
     * 查询所有医生列表
     */
    List<DoctorVO> listAll(DoctorListDTO dto);

    /**
     * 根据ID查询医生
     */
    DoctorVO getById(Long id);

    /**
     * 创建医生
     */
    void create(CreateDoctorDTO dto);

    /**
     * 更新医生
     */
    void update(Long id, UpdateDoctorDTO dto);

    /**
     * 删除医生
     */
    void remove(Long id);

    /**
     * 查询业务员在医院下的历史医生列表（用于订单创建时医生联想）
     */
    List<DoctorVO> listByCreatorAndHospital(DoctorSuggestDTO dto);

    /**
     * 快速添加医生
     * 如果医生已存在则返回现有医生，否则创建新医生记录
     */
    DoctorVO quickAdd(QuickAddDoctorDTO dto);

    /**
     * 修改状态
     */
    void updateStatus(Long id, Integer status);
}
