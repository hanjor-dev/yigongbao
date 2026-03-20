package com.yigongbao.module.basic.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
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
     * @param pageNum        页码
     * @param pageSize       每页条数
     * @param hospitalName   医院名称（模糊查询）
     * @param areaId         地区ID
     * @param hospitalLevel  医院等级
     * @param hospitalType   医院类型
     * @param status         状态
     * @return 分页后的医院列表
     */
    IPage<HospitalVO> listHospital(Integer pageNum, Integer pageSize, String hospitalName,
                                   Long areaId, Integer hospitalLevel, Integer hospitalType, Integer status);

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
}
