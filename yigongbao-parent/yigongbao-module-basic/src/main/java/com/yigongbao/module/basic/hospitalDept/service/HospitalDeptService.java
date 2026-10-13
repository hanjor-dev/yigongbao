package com.yigongbao.module.basic.hospitalDept.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.hospitalDept.dto.CreateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.dto.HospitalDeptListDTO;
import com.yigongbao.module.basic.hospitalDept.dto.HospitalDeptPageDTO;
import com.yigongbao.module.basic.hospitalDept.dto.UpdateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.entity.HospitalDeptEntity;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;

import java.util.List;

/**
 * 医院科室 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface HospitalDeptService extends IService<HospitalDeptEntity> {

    /**
     * 分页查询科室列表
     */
    IPage<HospitalDeptVO> listDepts(HospitalDeptPageDTO dto);

    /**
     * 查询所有科室列表
     */
    List<HospitalDeptVO> listAll(HospitalDeptListDTO dto);

    /**
     * 根据ID查询科室
     */
    HospitalDeptVO getById(Long id);

    /**
     * 创建科室
     */
    void create(CreateHospitalDeptDTO dto);

    /**
     * 更新科室
     */
    void update(Long id, UpdateHospitalDeptDTO dto);

    /**
     * 删除科室
     */
    void remove(Long id);

    /**
     * 修改状态
     */
    void updateStatus(Long id, Integer status);
}
