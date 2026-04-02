package com.yigongbao.module.basic.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.dto.HospitalPageDTO;
import com.yigongbao.module.basic.hospital.dto.UpdateHospitalDTO;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;

import java.util.List;

/**
 * 医院 Service 接口
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface HospitalService extends IService<HospitalEntity> {

    /**
     * 分页查询医院列表
     *
     * @param dto 分页查询参数
     * @return 分页后的医院列表
     */
    IPage<HospitalVO> listHospital(HospitalPageDTO dto);

    /**
     * 根据ID查询医院详情
     *
     * @param id 医院ID
     * @return 医院详情
     */
    HospitalVO getHospitalById(Long id);

    /**
     * 创建医院
     *
     * @param dto 创建参数
     */
    void createHospital(CreateHospitalDTO dto);

    /**
     * 更新医院
     *
     * @param id  医院ID
     * @param dto 更新参数
     */
    void updateHospital(Long id, UpdateHospitalDTO dto);

    /**
     * 修改医院状态
     *
     * @param id     医院ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取医院下拉选项
     *
     * @param status 状态筛选（null则不限制）
     * @return 医院下拉列表
     */
    List<HospitalVO> listOptions(Integer status);

    /**
     * 获取当前用户可操作的医院下拉选项（根据用户权限过滤）
     * 用于业务员创建订单等业务场景时的医院选择
     *
     * @param userId 用户ID
     * @return 当前用户可操作的医院列表
     */
    List<HospitalVO> listMyOptions(Long userId);
}
