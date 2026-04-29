package com.yigongbao.module.system.org.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.OrgPageDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.vo.OrgVO;

import com.yigongbao.module.system.org.vo.OrgHospitalChangeCheckVO;
import com.yigongbao.module.system.org.vo.OrgOperationCheckVO;

import java.util.List;

/**
 * 机构 Service 接口
 *
 * @author hanjor
 * @date 2026-03-16
 */
public interface OrgService extends IService<OrgEntity> {

    /**
     * 分页查询机构列表
     *
     * @param dto 分页查询参数
     * @return 分页后的机构列表
     */
    IPage<OrgVO> listOrg(OrgPageDTO dto);

    /**
     * 根据ID查询机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    OrgVO getOrgById(Long id);

    /**
     * 创建机构
     *
     * @param dto 创建参数
     */
    void createOrg(CreateOrgDTO dto);

    /**
     * 更新机构
     *
     * @param id  机构ID
     * @param dto 更新参数
     */
    void updateOrg(Long id, UpdateOrgDTO dto);

    /**
     * 删除机构
     *
     * @param id 机构ID
     */
    void removeOrg(Long id);

    /**
     * 修改机构状态
     *
     * @param id     机构ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 全量查询机构列表（用于前端下拉选择）
     *
     * @return 机构列表（包含字典名称）
     */
    List<OrgVO> listAllOrg();

    /**
     * 预检查经销商关联医院变更对用户权限的影响
     *
     * @param id             经销商机构ID
     * @param newHospitalIds 新的关联医院ID列表
     * @return 检查结果（affected=true 时需用户确认）
     */
    OrgHospitalChangeCheckVO checkHospitalChange(Long id, List<Long> newHospitalIds);

    /**
     * 预检查删除机构的影响（受影响用户、关联医生）
     *
     * @param id 机构ID
     * @return 检查结果
     */
    OrgOperationCheckVO checkRemove(Long id);

    /**
     * 预检查禁用机构的影响（受影响用户）
     *
     * @param id 机构ID
     * @return 检查结果
     */
    OrgOperationCheckVO checkDisable(Long id);
}
