package com.yigongbao.module.basic.registrationCert.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.registrationCert.dto.CreateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.dto.UpdateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.entity.RegistrationCertEntity;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;

import java.util.List;

/**
 * 注册证 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface RegistrationCertService extends IService<RegistrationCertEntity> {

    IPage<RegistrationCertVO> listCerts(Integer pageNum, Integer pageSize, String certCode, String certName, Integer status);

    List<RegistrationCertVO> listValidCerts();

    RegistrationCertVO getById(Long id);

    void create(CreateRegistrationCertDTO dto);

    void update(Long id, UpdateRegistrationCertDTO dto);

    void remove(Long id);

    void refreshExpiredStatus();

    /**
     * 根据ID列表批量查询（返回VO）
     *
     * @param ids ID列表
     * @return 注册证VO列表
     */
    List<RegistrationCertVO> listVOByIds(List<Long> ids);
}
